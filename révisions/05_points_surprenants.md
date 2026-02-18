# Fiche 05 — Points qui peuvent surprendre le prof

Cette fiche recense les choix techniques inhabituels ou avancés du code — les endroits où le prof peut poser des questions précises.

---

## 1. Classe imbriquée statique — `HallOfFameResponse.Entry`

```java
@Data
@AllArgsConstructor
public class HallOfFameResponse {
    private List<Entry> entries;

    @Data
    @AllArgsConstructor
    public static class Entry {       // ← static !
        private Long partieId;
        private String pseudo;
        private Integer score;
    }
}
```

**Q : Pourquoi `Entry` est une classe imbriquée dans `HallOfFameResponse` ?**  
R : Les deux sont fortement couplées conceptuellement (une `Entry` n'a de sens que dans le contexte du Hall of Fame). Les regrouper dans le même fichier améliore la lisibilité et évite la prolifération de petites classes.

**Q : Pourquoi `static` sur la classe imbriquée ?**  
R : Une classe imbriquée **non statique** (inner class) conserve implicitement une référence vers l'instance de la classe externe. Ici, `Entry` n'a pas besoin de `HallOfFameResponse` pour exister — elle ne partage aucune donnée d'instance avec la classe parente.  
`static` = **pas de référence cachée** vers l'objet parent → plus léger en mémoire, plus simple, peut être instancié sans créer d'abord une `HallOfFameResponse`.

**Règle simple** : toujours préférer `static` pour une classe imbriquée sauf si elle doit accéder aux membres d'instance de la classe externe.

---

## 2. `volatile` + `synchronized` ensemble dans `LobbyService`

```java
private volatile LobbyEntry waitingPlayer;

public synchronized LobbyReadyResponse ready(Long playerId) { ... }
public synchronized void cancelReady(Long playerId) { ... }
```

**Q : N'est-ce pas redondant ?**  
R : Techniquement oui. `synchronized` garantit déjà la visibilité mémoire (il implique un barrier mémoire). `volatile` est donc superflu ici. Mais `volatile` documente explicitement l'intention : "ce champ est partagé entre threads et sa valeur doit toujours être fraîche". C'est défensif et pédagogique.

**Q : Que se passerait-il sans `synchronized` ?**  
R : Si deux joueurs appellent `ready()` exactement en même temps, les deux peuvent lire `waitingPlayer == null` avant que l'un d'eux ait eu le temps de le setter. Les deux enregistreraient leur propre `LobbyEntry`, et aucune partie ne serait créée. Race condition classique.

**Q : Que se passerait-il sans `volatile` mais avec `synchronized` ?**  
R : Rien de grave ici car `synchronized` fournit la visibilité. Le `volatile` est vraiment superflu mais inoffensif.

---

## 3. Double système de statut de partie

```java
// Persisté en BDD (entité Parties)
enum PartieStatus { EN_COURS, TERMINE, ABANDON }

// En mémoire seulement (GameState)  
enum RuntimeGameStatus { WAITING, IN_PROGRESS, FINISHED }
```

**Q : Pourquoi deux enums pour la même chose ?**  
R : Ils ne représentent pas exactement la même chose :  
- `PartieStatus` est le statut **durable** (survit aux redémarrages), stocké en BDD, utilisé pour les requêtes, le Hall of Fame, etc.  
- `RuntimeGameStatus` est le statut **éphémère** du runtime en mémoire. Il permet de savoir si une partie a son état runtime disponible ou non.

C'est la séparation entre la couche **persistance** et la couche **état applicatif en mémoire**.

**Q : Que se passe-t-il si le serveur redémarre ?**  
R : Toutes les parties `EN_COURS` restent en BDD, mais leur `GameState` (dés, tour courant, timer) est perdu. Les joueurs ne peuvent plus jouer ces parties. C'est une limitation connue de cette architecture in-memory.

---

## 4. `@Builder.Default` — le piège Lombok + JPA

```java
@Column(name = "total_numbers", nullable = false)
@Builder.Default
private Integer totalNumbers = 0;
```

**Q : Que se passe-t-il sans `@Builder.Default` ?**  
R : Quand on utilise `ColonneScore.builder().idPartie(1L).idJoueur(2L).build()`, Lombok génère un builder qui **ignore** la valeur initiale `= 0`. Le champ se retrouve à `null`. Comme la colonne est `nullable = false`, Hibernate lèvera une `ConstraintViolationException` à la sauvegarde.

`@Builder.Default` est obligatoire dès qu'un champ a une valeur par défaut ET qu'on utilise le Builder.

---

## 5. FK manuelles sans `@ManyToOne` — un choix délibéré

```java
// Dans ColonneScore — pas de @ManyToOne, juste des Long
@Column(name = "id_partie", nullable = false)
private Long idPartie;

@Column(name = "id_joueur", nullable = false)
private Long idJoueur;
```

**Q : N'est-ce pas contre les bonnes pratiques JPA ?**  
R : JPA recommande les associations (`@ManyToOne`), mais elles apportent des complications :
- **Lazy loading** : Hibernate peut faire des requêtes supplémentaires non voulues
- **Sérialisation JSON** : risque de boucles infinies si les deux côtés s'incluent mutuellement
- **`@JsonIgnore` / `@JsonManagedReference`** : annotations supplémentaires à gérer

Ici, le choix de garder des `Long` simplifie le code au prix d'un join explicite en JPQL. C'est un arbitrage valide, surtout pour un projet de taille modeste.

---

## 6. `Function<Claims, T>` dans `JwtService` — généricité

```java
public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
}

// Usage :
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);  // référence de méthode
}
```

**Q : Pourquoi utiliser `Function<Claims, T>` ?**  
R : Pour éviter de dupliquer la logique de parsing du token. On factorise l'extraction des claims dans `extractClaim`, et chaque appel spécifie juste quelle propriété extraire via une lambda ou une référence de méthode. `Claims::getSubject` est équivalent à `claims -> claims.getSubject()`.

---

## 7. `EnumMap` dans `ScoreService.possibleScores()`

```java
Map<Category, Integer> result = new EnumMap<>(Category.class);
```

**Q : Pourquoi `EnumMap` et pas `HashMap` ?**  
R : `EnumMap` est une implémentation spécialisée pour les clés de type enum. Elle utilise un simple tableau interne indexé par l'ordinal de l'enum → **plus rapide** et **moins de mémoire** qu'un `HashMap`. À utiliser systématiquement quand les clés sont des enums.

---

## 8. `@Component` vs `@Service` vs `@Bean`

| Annotation | Où | Rôle |
|---|---|---|
| `@Service` | Classe de service | Stéréotype métier (sémantique) — idem `@Component` fonctionnellement |
| `@Component` | Autre composant Spring | Générique (ex: `GameStateManager`) |
| `@Configuration` + `@Bean` | Classe de config | Déclare des beans manuellement (ex: `PasswordEncoder`, `SecurityFilterChain`) |

**Q : Quelle est la vraie différence entre `@Service` et `@Component` ?**  
R : Aucune différence technique — les deux enregistrent un bean dans le contexte Spring. `@Service` est une annotation de stéréotype qui communique l'intention (couche métier). Elle est également détectable par des outils/AOP.

---

## 9. `CommandLineRunner` vs `ApplicationRunner`

Dans `DataSeeder`, on utilise `CommandLineRunner` :
```java
@Bean
CommandLineRunner seedUsers() {
    return args -> { ... };  // args = String[] des arguments CLI
}
```

**Q : Quelle alternative existe ?**  
R : `ApplicationRunner` reçoit un `ApplicationArguments` au lieu d'un `String[]` — plus pratique si on veut parser les arguments nommés. Pour du seeding simple, `CommandLineRunner` est suffisant.

---

## 10. `Math.max(1, Math.min(limit, 100))` — clamp de valeur

```java
int normalizedLimit = Math.max(1, Math.min(limit, 100));
```

**Q : Que fait cette ligne ?**  
R : C'est un **clamp** : force la valeur dans l'intervalle [1, 100]. `Math.min(limit, 100)` empêche de dépasser 100. `Math.max(1, ...)` empêche d'aller en dessous de 1. Protège contre des paramètres malveillants ou incohérents (`?limit=-5` ou `?limit=999999`).

---

## Résumé des "pièges" à ne pas se faire avoir

| Sujet | Point clé |
|---|---|
| Classe imbriquée `static` | Pas de référence vers la classe externe — toujours préférer `static` |
| `volatile` + `synchronized` | `synchronized` seul aurait suffi ; `volatile` est défensif |
| `Integer` vs `int` | `null` = case non jouée, `0` = case jouée avec score nul |
| `@Builder.Default` | Obligatoire si valeur par défaut + usage du Builder |
| `EnumType.STRING` | Robuste si l'enum est réordonné ; `ORDINAL` est fragile |
| FK manuelles | Choix délibéré contre le lazy loading et les boucles JSON |
| H2 in-memory | Données perdues au redémarrage — OK pour démo, pas pour prod |
