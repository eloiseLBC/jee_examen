# 🎲 Yam Multijoueur


Application web de **Yam en temps réel à deux joueurs**, construite avec Spring Boot (backend) et Vue.js (frontend).

---

## 📋 Table des matières

- [Aperçu](#aperçu)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Lancer le projet](#lancer-le-projet)
- [API REST](#api-rest)
- [Règles du jeu](#règles-du-jeu)
- [Structure du projet](#structure-du-projet)

---

## Aperçu

L'application permet à deux joueurs de s'affronter en ligne au Yam :

- **Inscription / Connexion** sécurisée via JWT
- **Lobby** de matchmaking automatique (deux joueurs se connectent, la partie démarre)
- **Partie en temps réel** avec gestion des tours, lancers de dés, verrouillage et scoring
- **Timeout automatique** : si un joueur ne joue pas dans les 30 secondes, une catégorie est pénalisée à 0
- **Hall of Fame** : classement des meilleures parties terminées

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Java 17 + Spring Boot 3 |
| Sécurité | Spring Security + JWT (jjwt) |
| Persistance | Spring Data JPA + Hibernate |
| Base de données | PostgreSQL (Docker) |
| Frontend | Vue.js 3 |
| Containerisation | Docker + Docker Compose |
| Build | Maven Wrapper (`./mvnw`) |

---

## Architecture

Le backend suit une architecture en couches classique :

```
controller  →  service  →  repository  →  entity (BDD)
                ↕
           runtime (état en mémoire)
```

- **`entity`** : entités JPA persistées (`Joueur`, `Parties`, `ColonneScore`)
- **`repository`** : interfaces Spring Data JPA
- **`service`** : logique métier (auth, jeu, lobby, scoring, JWT)
- **`runtime`** : état volatile en mémoire (`GameState`, `LobbyEntry`) — non persisté
- **`dto`** : objets de transfert entre le frontend et le backend
- **`enums`** : `Category`, `PartieStatus`, `RuntimeGameStatus`
- **`controller`** : endpoints REST

---

## Prérequis

- [Docker](https://www.docker.com/) et Docker Compose
- Java 17+ (optionnel si vous utilisez uniquement Docker)
- Node.js 18+ (pour lancer le frontend hors Docker)

---

## Lancer le projet

### Avec Docker Compose (recommandé)

```bash
# Cloner le dépôt
git clone https://github.com/eloiseLBC/jee_examen.git
cd jee_examen

# Lancer l'ensemble (backend + base de données + frontend)
docker-compose up --build
```

L'application sera disponible sur :
- Frontend : [http://localhost:5173](http://localhost:5173)
- Backend API : [http://localhost:8080](http://localhost:8080)

### Sans Docker (développement)

```bash
# Démarrer uniquement la base de données
docker-compose up db -d

# Lancer le backend
./mvnw spring-boot:run

# Lancer le frontend (dans un autre terminal)
cd frontend
npm install
npm run dev
```

---

## API REST

Tous les endpoints (sauf `/auth/**`) nécessitent un header :
```
Authorization: Bearer <token>
```

### Authentification — `/auth`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/auth/register` | Créer un compte |
| `POST` | `/auth/login` | Se connecter, retourne un JWT |

**Exemple register :**
```json
POST /auth/register
{
  "pseudo": "joueur1",
  "password": "monmotdepasse"
}
```

**Exemple login :**
```json
POST /auth/login
{
  "pseudo": "joueur1",
  "password": "monmotdepasse"
}
// Réponse : { "token": "eyJ..." }
```

---

### Lobby — `/lobby`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/lobby/ready` | Se mettre en attente d'un adversaire |
| `DELETE` | `/lobby/ready` | Annuler l'attente |

La réponse de `POST /lobby/ready` indique si un match a été trouvé :
```json
// En attente
{ "matched": false, "gameId": null, "expiresInSec": 58 }

// Match trouvé
{ "matched": true, "gameId": 42, "expiresInSec": null }
```

> Le frontend doit **poller** cet endpoint régulièrement pour détecter le match.

---

### Jeu — `/games`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/games/{gameId}` | État complet de la partie |
| `POST` | `/games/{gameId}/roll` | Lancer tous les dés (premier lancer du tour) |
| `POST` | `/games/{gameId}/lock` | Verrouiller des dés et relancer |
| `POST` | `/games/{gameId}/score` | Valider un score pour une catégorie |

**Exemple lock & roll :**
```json
POST /games/42/lock
{
  "lockedIndexes": [0, 2, 4]
}
```

**Exemple score :**
```json
POST /games/42/score
{
  "category": "BRELAN"
}
```

---

### Hall of Fame — `/halloffame`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/halloffame?limit=10` | Top N des meilleurs scores |

---

## Règles du jeu

Chaque tour, le joueur actif dispose de **3 lancers maximum** et **30 secondes**.

### Catégories disponibles

| Catégorie | Points |
|-----------|--------|
| Un à Six | Somme des dés correspondants |
| Brelan | Somme de tous les dés (3 identiques min.) |
| Carré | Somme de tous les dés (4 identiques min.) |
| Full | 25 points (3 d'un + 2 d'un autre) |
| Petite suite | 30 points (4 dés consécutifs) |
| Grande suite | 40 points (5 dés consécutifs) |
| Yam | 50 points (5 identiques) |
| Chance | Somme de tous les dés |

**Bonus :** +35 points si la somme des catégories 1-6 ≥ 63.

**Yam supplémentaire :** chaque Yam obtenu après que la case Yam est déjà remplie rapporte +100 points.

**Timeout :** si le joueur ne valide pas dans les 30 secondes, la première catégorie libre est remplie avec 0.

La partie se termine dès qu'un joueur a rempli toutes ses catégories. Le joueur avec le score total le plus élevé gagne.

---

## Structure du projet

```
jee_examen/
├── src/main/java/com/example/jee/examen/
│   ├── controller/          # Endpoints REST
│   │   ├── AuthController
│   │   ├── GameController
│   │   ├── HallOfFameController
│   │   └── LobbyController
│   ├── service/             # Logique métier
│   │   ├── AuthService
│   │   ├── GameService
│   │   ├── LobbyService
│   │   ├── ScoreService
│   │   ├── DiceService
│   │   ├── HallOfFameService
│   │   ├── JwtService
│   │   ├── AppUserDetailsService
│   │   ├── AuthenticatedUserService
│   │   ├── GameStateManager
│   │   └── HallOfFameRow
│   ├── entity/              # Entités JPA
│   │   ├── Joueur
│   │   ├── Parties
│   │   └── ColonneScore
│   ├── repository/          # Interfaces Spring Data JPA
│   │   ├── JoueurRepository
│   │   ├── PartiesRepository
│   │   └── ColonneScoreRepository
│   ├── runtime/             # État en mémoire (non persisté)
│   │   ├── GameState
│   │   └── LobbyEntry
│   ├── dto/                 # Objets de transfert
│   │   ├── AuthLoginRequest / AuthRegisterRequest / AuthResponse
│   │   ├── GameResponse / RollResponse / ScoreSheetDto
│   │   ├── LobbyReadyResponse / LockRequest / ScoreRequest
│   │   └── HallOfFameResponse
│   └── enums/
│       ├── Category
│       ├── PartieStatus
│       └── RuntimeGameStatus
├── frontend/                # Application Vue.js
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```
