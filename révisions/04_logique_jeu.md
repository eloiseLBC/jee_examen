# Fiche 04 — Logique de jeu

## Le Lobby — mise en file d'attente

### Fonctionnement
Le lobby permet à deux joueurs de se trouver sans connaissance préalable l'un de l'autre.

```
Joueur A → POST /lobby/ready  → entre en attente (LobbyEntry stocké en mémoire)
Joueur B → POST /lobby/ready  → trouve A en attente → crée une partie → renvoie gameId aux deux
```

### `LobbyService` — code clé

```java
private volatile LobbyEntry waitingPlayer;

public synchronized LobbyReadyResponse ready(Long playerId) {
    long now = System.currentTimeMillis();
    cleanupIfExpired(now);            // expire après 60s

    // Cas 1 : c'est le même joueur qui re-poll → renvoie "toujours en attente"
    if (waitingPlayer != null && waitingPlayer.getPlayerId().equals(playerId)) { ... }

    // Cas 2 : personne n'attend → enregistrer ce joueur
    if (waitingPlayer == null) {
        waitingPlayer = LobbyEntry.builder()...build();
        return LobbyReadyResponse.builder().matched(false).build();
    }

    // Cas 3 : quelqu'un attend déjà → créer la partie !
    Long opponentId = waitingPlayer.getPlayerId();
    waitingPlayer = null;
    Long gameId = gameService.createGame(opponentId, playerId);
    return LobbyReadyResponse.builder().matched(true).gameId(gameId).build();
}
```

**Q : Pourquoi `volatile` sur `waitingPlayer` ?**  
R : `volatile` garantit que toutes les lectures/écritures sur ce champ sont visibles par tous les threads immédiatement (pas de cache CPU local). Sans `volatile`, un thread pourrait lire une valeur obsolète du champ.

**Q : Pourquoi `synchronized` en plus ?**  
R : `volatile` ne suffit pas pour les opérations composées (lire + comparer + écrire). Deux joueurs pourraient appeler `ready()` simultanément et créer deux parties. `synchronized` sur la méthode garantit qu'un seul thread à la fois exécute ce bloc.

**Q : Pourquoi les deux ensemble (`volatile` + `synchronized`) ?**  
R : C'est une redondance de sécurité. Dans ce cas, `synchronized` seul aurait suffi car il implique la visibilité mémoire. `volatile` est conservatoire, pour être explicite.

---

## GameStateManager — État en mémoire des parties

```java
@Component
public class GameStateManager {
    private final Map<Long, GameState> states = new ConcurrentHashMap<>();
    ...
}
```

**Q : Pourquoi `ConcurrentHashMap` et pas `HashMap` ?**  
R : `HashMap` n'est pas thread-safe. Si deux requêtes lisent/écrivent simultanément, on peut avoir une corruption de données. `ConcurrentHashMap` est conçu pour les accès concurrents : il segmente la map en partitions verrouillées indépendamment.

**Q : Pourquoi `Optional.ofNullable(states.get(gameId))` ?**  
R : `get()` retourne `null` si la clé n'existe pas. `Optional` force l'appelant à gérer explicitement l'absence de valeur, évitant les `NullPointerException`. Le service fait `.orElseThrow(...)` pour renvoyer une 404 propre.

---

## GameState — L'état d'une partie en mémoire

```java
private Long partieId;
private List<Long> playerIds;      // [joueurA, joueurB]
private Long currentPlayerId;      // à qui c'est le tour
private int[] dice;                // [d1, d2, d3, d4, d5]
private boolean[] locked;          // dés gardés
private int rollCount;             // 0, 1, 2 ou 3
private long turnStartedAt;        // timestamp ms
private long turnDeadlineAt;       // timestamp ms (tournStart + 30s)
private Map<Long, Integer> extraYamCount; // Yams bonus
private RuntimeGameStatus status;
```

---

## Flow d'un tour de jeu

```
1. roll()           → rollCount 0→1 : lance tous les dés
2. lockAndRoll()    → rollCount 1→2 : garde certains dés, relance les autres
3. lockAndRoll()    → rollCount 2→3 : dernier lancer (max 3 lancers)
4. score()          → le joueur choisit une catégorie → tour terminé
```

Règle : 3 lancers maximum par tour. Après le 3e lancer, `score()` est obligatoire.

**Q : Que se passe-t-il si le joueur dépasse le temps imparti (30s) ?**  
R : `applyTimeoutPenaltyIfNeeded()` est appelé en début de chaque action. Si `System.currentTimeMillis() > turnDeadlineAt`, la **première catégorie non remplie** de sa feuille est forcée à **0**, et le tour passe au joueur suivant.

---

## DiceService — Lancer les dés

```java
private final Random random = new Random();

private int rollDie() {
    return random.nextInt(6) + 1;  // 1 à 6 inclus
}

public int[] rerollUnlocked(int[] currentDice, boolean[] locked) {
    int[] next = currentDice.clone();  // copie pour ne pas modifier l'original
    for (int i = 0; i < 5; i++) {
        if (!locked[i]) {
            next[i] = rollDie();
        }
    }
    return next;
}
```

**Q : Pourquoi `currentDice.clone()` ?**  
R : Les tableaux Java sont des références. Sans `.clone()`, on modifierait directement le tableau du `GameState` pendant le calcul. On travaille sur une copie pour l'atomicité.

---

## ScoreService — Calcul des scores

### Le switch sur enum
```java
return switch (cat) {
    case ONE   -> counts[1] * 1;
    case TWO   -> counts[2] * 2;
    case BRELAN -> hasAtLeastNOfAKind(counts, 3) ? sum : 0;
    case FULL  -> isFull(counts) ? 25 : 0;
    case YAM   -> hasAtLeastNOfAKind(counts, 5) ? 50 : 0;
    case CHANCE -> sum;
    ...
};
```

### La méthode `counts()`
```java
int[] counts = new int[7]; // index 0 inutilisé, index 1-6 = nb de dés avec cette valeur
for (int die : dice) {
    counts[die]++;
}
```
Astuce : un tableau de taille 7 indexé par la valeur du dé. Rapide et élégant.

### Règles de score à connaître
| Catégorie | Condition | Score |
|---|---|---|
| ONE..SIX | toujours | somme des dés de cette valeur |
| BRELAN | ≥ 3 dés identiques | somme totale des 5 dés |
| CARRÉ | ≥ 4 dés identiques | somme totale des 5 dés |
| FULL | 3 d'une valeur + 2 d'une autre | 25 fixe |
| PETITE_SUITE | 4 valeurs consécutives | 30 fixe |
| GRANDE_SUITE | 5 valeurs consécutives | 40 fixe |
| YAM | 5 dés identiques | 50 fixe |
| CHANCE | toujours | somme totale des 5 dés |

### Bonus chiffres (≥ 63 points dans les cases ONE..SIX)
```java
int totalNumbers = n1 + n2 + n3 + n4 + n5 + n6;
int bonus = totalNumbers >= 63 ? 35 : 0;
```
Atteindre 63 = avoir en moyenne 3 fois chaque chiffre (3×1 + 3×2 + ... + 3×6 = 63).

### Extra Yam
Si un joueur fait un Yam alors que sa case YAM est déjà remplie → `extraYamCount` est incrémenté. Chaque Yam bonus vaut **+100 points** dans le total.

---

## completeTurnOrFinish — Fin de tour ou fin de partie

```java
boolean finished = sheets.stream().allMatch(scoreService::allCategoriesFilled);
```

**Q : Que fait `allMatch` avec une référence de méthode ?**  
R : `allMatch` retourne `true` si **tous** les éléments du stream vérifient le prédicat. Ici, `scoreService::allCategoriesFilled` est une référence de méthode équivalente à `sheet -> scoreService.allCategoriesFilled(sheet)`.

Quand la partie est finie :
1. Le vainqueur = le joueur avec le `scoreTotal` le plus élevé (`max(Comparator.comparingInt(...))`)
2. `partie.setStatus(PartieStatus.TERMINE)` + `partie.setIdVainqueur(...)` → sauvegardé en BDD
3. `state.setStatus(RuntimeGameStatus.FINISHED)` → mis à jour en mémoire

---

## switchToNextPlayer — Passage du tour

```java
int currentIdx = ids.indexOf(state.getCurrentPlayerId());
int nextIdx = (currentIdx + 1) % ids.size();  // modulo pour boucler
state.setCurrentPlayerId(ids.get(nextIdx));
```

Le modulo `% ids.size()` permet de revenir au joueur 0 après le joueur 1 (jeu en tour par tour circulaire). Le timer de 30 secondes est également réinitialisé.

---

## Tests — ScoreServiceTest

```java
@BeforeEach
void setUp() {
    scoreService = new ScoreService();  // instanciation directe, pas de Spring
}
```

**Q : Pourquoi pas `@SpringBootTest` ici ?**  
R : `ScoreService` n'a aucune dépendance externe (pas de BDD, pas de réseau). Un test **unitaire pur** suffit — plus rapide et plus isolé. `@SpringBootTest` démarrerait tout le contexte Spring inutilement.
