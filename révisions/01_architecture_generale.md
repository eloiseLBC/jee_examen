# Fiche 01 — Architecture générale

## Vue d'ensemble

C'est une application **Spring Boot 3** (Java 17) qui implémente un jeu de **Yam multijoueur** (2 joueurs).  
Le frontend Vue.js est découplé et communique avec le backend via une **API REST JSON**.

---

## Structure des packages

```
config/       → Configuration Spring (Security, CORS, DataSeeder, OpenAPI)
controller/   → Couche HTTP : reçoit les requêtes, délègue aux services
dto/          → Objets de transfert (entrée/sortie de l'API, jamais persistés)
entity/       → Objets JPA mappés en base de données
enums/        → Énumérations partagées
repository/   → Accès BDD via Spring Data JPA
runtime/      → État en mémoire (pas persisté)
service/      → Logique métier
```

---

## Les 3 couches principales

| Couche | Rôle | Exemple |
|---|---|---|
| **Controller** | Reçoit HTTP, lit l'utilisateur courant, renvoie DTO | `GameController` |
| **Service** | Logique métier, validations, orchestration | `GameService`, `ScoreService` |
| **Repository** | CRUD base de données | `ColonneScoreRepository` |

---

## Endpoints REST

| Méthode | URL | Rôle | Auth |
|---|---|---|---|
| POST | `/auth/register` | Créer un compte | ❌ |
| POST | `/auth/login` | Se connecter, reçoit un JWT | ❌ |
| POST | `/lobby/ready` | Se mettre en attente d'adversaire | ✅ |
| DELETE | `/lobby/ready` | Annuler l'attente | ✅ |
| GET | `/games/{id}` | État d'une partie | ✅ |
| POST | `/games/{id}/roll` | Lancer les dés | ✅ |
| POST | `/games/{id}/lock` | Garder des dés et relancer | ✅ |
| POST | `/games/{id}/score` | Choisir une catégorie à scorer | ✅ |
| GET | `/halloffame?limit=10` | Classement des meilleurs scores | ✅ |

---

## Les deux systèmes de statut

> **Point important** : il existe deux enums de statut, qui représentent deux niveaux d'information.

### `PartieStatus` (persisté en BDD — entité `Parties`)
```java
EN_COURS, TERMINE, ABANDON
```
C'est ce qui est stocké en base. La BDD ne connaît que l'état "macro" de la partie.

### `RuntimeGameStatus` (en mémoire — `GameState`)
```java
WAITING, IN_PROGRESS, FINISHED
```
C'est l'état fin-grain du runtime (dés, tour en cours, etc.), stocké dans `GameStateManager` via une `ConcurrentHashMap`. **Non persisté** : si le serveur redémarre, les parties en cours sont perdues.

---

## DTOs vs Entités

**Entités** (`entity/`) = objets liés à la base de données (`@Entity`). On ne les expose jamais directement à l'API.  
**DTOs** (`dto/`) = ce qu'on envoie/reçoit dans les requêtes HTTP. Découple la BDD du contrat d'API.

Exemples :
- `AuthLoginRequest` → corps de la requête POST `/auth/login`
- `GameResponse` → réponse renvoyée au client avec l'état complet de la partie
- `ScoreSheetDto` → feuille de score formatée pour le frontend

---

## DataSeeder — Initialisation au démarrage

```java
@Bean
CommandLineRunner seedUsers() {
    return args -> {
        seed("alice", "password");
        seed("bob", "password");
        seed("charlie", "password");
    };
}
```

**Q : Qu'est-ce que `CommandLineRunner` ?**  
R : C'est une interface Spring Boot. La méthode `run()` est appelée automatiquement après le démarrage du contexte applicatif. Utile pour insérer des données de test. Le `@Bean` retourne une lambda qui implémente `CommandLineRunner`.

---

## Q / R rapides

**Q : Pourquoi `@RestController` et pas `@Controller` ?**  
R : `@RestController` = `@Controller` + `@ResponseBody` sur toutes les méthodes. Toutes les réponses sont automatiquement sérialisées en JSON.

**Q : Pourquoi utiliser `ResponseStatusException` dans les services ?**  
R : Permet de renvoyer directement un code HTTP (404, 400, 403…) depuis n'importe quelle couche sans créer des exceptions custom. Spring la convertit automatiquement en réponse HTTP.

**Q : À quoi sert `@Transactional(readOnly = true)` ?**  
R : Optimisation : indique à Hibernate qu'il ne faut pas détecter les changements (flush) ni ouvrir de verrous en écriture. Plus performant pour les lectures pures.

**Q : Qu'est-ce que `@RequiredArgsConstructor` (Lombok) ?**  
R : Génère un constructeur avec tous les champs `final`. Spring utilise ce constructeur pour l'injection de dépendances (injection par constructeur, recommandée).
