# Fiche 02 — Sécurité & JWT

## Flow d'authentification complet

```
1. POST /auth/register  → mot de passe haché avec BCrypt → sauvé en BDD
2. POST /auth/login     → AuthenticationManager vérifie pseudo+mdp
                       → JwtService génère un token signé
                       → token renvoyé au client dans AuthResponse
3. Chaque requête suivante :
   Header: Authorization: Bearer <token>
   → JwtAuthenticationFilter extrait et valide le token
   → met l'utilisateur dans le SecurityContext
   → le contrôleur peut alors appeler authenticatedUserService.currentUser()
```

---

## JwtService — Création et validation des tokens

```java
public String generateToken(String username) {
    return Jwts.builder()
            .subject(username)       // payload : le pseudo
            .issuedAt(now)           // iat : date d'émission
            .expiration(expiry)      // exp : expiration (24h)
            .signWith(getSignInKey()) // signature HMAC-SHA
            .compact();
}
```

**Q : Qu'est-ce qu'un JWT ?**  
R : JSON Web Token. Composé de 3 parties base64 séparées par des points :  
`header.payload.signature`  
Le serveur peut vérifier la signature sans base de données. Le token est auto-porteur.

**Q : Que contient le payload ici ?**  
R : Uniquement le `subject` (pseudo du joueur), la date d'émission (`iat`) et la date d'expiration (`exp`). Pas de rôles dans le token.

**Q : Comment la clé de signature est-elle gérée ?**  
R : Elle est lue depuis `application.properties` (`app.jwt.secret`), décodée depuis Base64, et transformée en `SecretKey` HMAC-SHA via `Keys.hmacShaKeyFor()`. La bibliothèque utilisée est **JJWT 0.12.6**.

**Q : Quelle est la durée de vie du token ?**  
R : `app.jwt.expiration-ms=86400000` → 86 400 000 ms = **24 heures**.

---

## JwtAuthenticationFilter — Le filtre HTTP

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

**Q : Pourquoi `OncePerRequestFilter` ?**  
R : Garantit que le filtre n'est exécuté **qu'une seule fois par requête**, même si la chaîne de filtres est réinvoquée (ce qui peut arriver avec les forwards internes Spring).

**Q : Comment le filtre fonctionne-t-il étape par étape ?**  
1. Lit le header `Authorization`
2. S'il ne commence pas par `"Bearer "` → passe au filtre suivant sans authentifier
3. Extrait le token (substring(7) pour enlever "Bearer ")
4. Extrait le `username` depuis le token
5. Si l'utilisateur n'est pas encore authentifié dans le `SecurityContext` :
   - charge le `UserDetails` depuis la BDD
   - vérifie la validité du token (signature + expiration)
   - crée un `UsernamePasswordAuthenticationToken` et le place dans le `SecurityContext`
6. Dans tous les cas → `filterChain.doFilter()` continue la chaîne

**Q : Pourquoi vérifier `SecurityContextHolder.getContext().getAuthentication() == null` ?**  
R : Évite de recharger l'utilisateur depuis la BDD si la requête a déjà été authentifiée (ex : 2 filtres appliqués).

---

## SecurityConfig — Configuration de la chaîne de sécurité

**Q : Que fait `SessionCreationPolicy.STATELESS` ?**  
R : Désactive la création de session HTTP côté serveur. Chaque requête doit être auto-authentifiée via le JWT. C'est le mode standard pour les API REST.

**Q : Pourquoi CSRF est-il désactivé ?**  
R : CSRF protège les navigateurs qui envoient des cookies de session. Comme on utilise des JWT dans le header `Authorization`, il n'y a pas de session ni de cookie d'auth → pas de risque CSRF → on peut désactiver.

**Q : Que font ces lignes ?**  
```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```
R : Les requêtes préflight CORS (méthode OPTIONS) ne portent pas de JWT. Il faut les laisser passer sans auth sinon le navigateur bloque tout.

**Q : Pourquoi `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` ?**  
R : Sans ça, Spring renverrait **403 Forbidden** quand l'utilisateur n'est pas authentifié. Or, **401 Unauthorized** est la réponse correcte quand il n'y a pas d'identité. Le frontend peut ainsi distinguer "non connecté" (401) de "connecté mais sans droits" (403).

**Q : Pourquoi `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` ?**  
R : On insère notre filtre JWT **avant** le filtre de login par formulaire (qui n'est de toute façon pas utilisé ici). Cela assure que l'authentification JWT est traitée avant toute autre vérification.

---

## DaoAuthenticationProvider

```java
DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
authProvider.setUserDetailsService(userDetailsService);
authProvider.setPasswordEncoder(passwordEncoder());
```

**Q : Quel est son rôle ?**  
R : Il orchestre l'authentification par formulaire classique (login/mot de passe). Il charge le `UserDetails` via `AppUserDetailsService`, puis compare le mot de passe reçu (brut) avec le hash stocké via `BCryptPasswordEncoder`. C'est lui qui est utilisé lors du `POST /auth/login`.

---

## AppUserDetailsService

```java
return User.withUsername(joueur.getPseudo())
        .password(joueur.getMdp())
        .roles("USER")
        .build();
```

**Q : Pourquoi implémenter `UserDetailsService` ?**  
R : C'est l'interface que Spring Security appelle pour charger un utilisateur par son identifiant. On la surcharge pour aller chercher dans notre table `joueur` (et pas dans une table `users` par défaut).

---

## AuthenticatedUserService — Récupérer l'utilisateur courant

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
return joueurRepository.findByPseudo(auth.getName())...
```

**Q : Qu'est-ce que le `SecurityContextHolder` ?**  
R : C'est un stockage thread-local. Après que `JwtAuthenticationFilter` a positionné l'authentification, n'importe quel code dans le même thread peut y accéder. `auth.getName()` retourne le pseudo (le `subject` du JWT).

**Q : Pourquoi refaire un appel BDD alors que le JWT contient le pseudo ?**  
R : Pour obtenir l'**entité `Joueur` complète** (avec son `id`), nécessaire pour les vérifications métier. Le JWT ne contient que le pseudo, pas l'id.
