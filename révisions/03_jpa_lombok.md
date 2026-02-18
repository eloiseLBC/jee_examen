# Fiche 03 — JPA & Lombok

## Les entités JPA

Il y a 3 entités persistées en base :

| Entité | Table | Rôle |
|---|---|---|
| `Joueur` | `joueur` | Compte d'un joueur (pseudo + mdp hashé) |
| `Parties` | `parties` | Une partie (statut + id vainqueur) |
| `ColonneScore` | `colonneScore` | Feuille de score d'un joueur pour une partie |

---

## Annotations JPA clés

### `@Entity` et `@Table`
```java
@Entity
@Table(name = "colonneScore")
public class ColonneScore { ... }
```
`@Entity` : déclare la classe comme entité JPA (Hibernate crée la table correspondante).  
`@Table(name = "...")` : spécifie le nom de la table en BDD (utile quand il diffère du nom de la classe).

### `@Id` et `@GeneratedValue`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
**Q : Que fait `GenerationType.IDENTITY` ?**  
R : La BDD auto-incrémente l'id (ex : `AUTO_INCREMENT` MySQL, `SERIAL` PostgreSQL). C'est la stratégie la plus simple et la plus commune. Alternative : `SEQUENCE` (séquences BDD), `TABLE` (table dédiée aux compteurs).

### `@Column`
```java
@Column(name = "id_partie", nullable = false)
private Long idPartie;

@Column(name = "score_1")
private Integer score1;  // nullable → score pas encore joué
```
**Q : Pourquoi `Integer` (objet) et pas `int` (primitif) pour les scores ?**  
R : `int` ne peut pas être `null`. Or, un score non encore joué doit être `null` pour le distinguer d'un score de 0. Si un joueur score 0 dans une case, `score1 = 0`. Si la case n'est pas encore jouée, `score1 = null`. L'absence de valeur est significative.

### `@Enumerated(EnumType.STRING)`
```java
@Enumerated(EnumType.STRING)
private PartieStatus status;
```
**Q : Pourquoi `EnumType.STRING` et pas `EnumType.ORDINAL` (valeur par défaut) ?**  
R : `ORDINAL` stocke l'index (0, 1, 2…). Si on réordonne l'enum, les données existantes deviennent incohérentes. `STRING` stocke le nom (`"EN_COURS"`, `"TERMINE"`), beaucoup plus robuste et lisible en BDD.

---

## Les relations — absence de `@ManyToOne`

**Q : Pourquoi `ColonneScore` n'a pas de `@ManyToOne Parties` ni `@ManyToOne Joueur` ?**  
R : Les FK sont gérées **manuellement** via des `Long idPartie` et `Long idJoueur`. C'est un choix délibéré d'éviter les relations JPA bidirectionnelles (qui peuvent provoquer des problèmes de lazy loading, de boucles de sérialisation JSON, etc.). La jointure se fait en JPQL à la demande.

---

## Spring Data JPA — JpaRepository

```java
public interface ColonneScoreRepository extends JpaRepository<ColonneScore, Long> {
    Optional<ColonneScore> findByIdPartieAndIdJoueur(Long idPartie, Long idJoueur);
    boolean existsByIdPartieAndIdJoueur(Long idPartie, Long idJoueur);
    List<ColonneScore> findByIdPartieOrderByIdJoueurAsc(Long idPartie);
}
```

**Q : Comment ces méthodes marchent sans implémentation ?**  
R : Spring Data JPA **génère automatiquement** le SQL à partir du nom de la méthode (méthodes dérivées). `findByIdPartieAndIdJoueur` → `SELECT * FROM colonneScore WHERE id_partie = ? AND id_joueur = ?`.

**Q : Quelles méthodes `JpaRepository` fournit-il gratuitement ?**  
R : `save()`, `findById()`, `findAll()`, `deleteById()`, `existsById()`, `count()`, etc. (hérité de `CrudRepository`).

---

## Requête JPQL custom — Hall of Fame

```java
@Query("""
    select new com.example.jee.examen.service.HallOfFameRow(c.idPartie, j.pseudo, c.scoreTotal)
    from ColonneScore c
    join Joueur j on j.id = c.idJoueur
    join Parties p on p.id = c.idPartie
    where p.status = :status
    order by c.scoreTotal desc
    """)
List<HallOfFameRow> findTopByPartieStatus(PartieStatus status, Pageable pageable);
```

**Q : Qu'est-ce que ce `new HallOfFameRow(...)` dans la requête JPQL ?**  
R : C'est un **constructor expression** JPQL. Plutôt que de retourner des entités complètes, on instancie directement un objet `HallOfFameRow` avec les champs sélectionnés. Il faut le chemin complet de la classe.

**Q : Pourquoi `Pageable` en paramètre ?**  
R : Pour limiter le nombre de résultats côté BDD (SQL `LIMIT`) sans charger toute la table. `PageRequest.of(0, limit)` → page 0, taille `limit`. On récupère une `List<>` et pas une `Page<>` car on n'a pas besoin du compte total.

**Q : Pourquoi faire un `join Joueur` et un `join Parties` sans `@ManyToOne` ?**  
R : JPQL permet le join sur des conditions arbitraires avec `join X on X.id = Y.idX`. Puisqu'il n'y a pas de relation JPA déclarée, on utilise cette syntaxe explicite.

---

## Lombok

Lombok génère du code à la compilation (via annotation processor). Il évite le boilerplate.

| Annotation | Ce qu'elle génère |
|---|---|
| `@Getter` | Tous les getters |
| `@Setter` | Tous les setters |
| `@NoArgsConstructor` | Constructeur sans paramètres (requis par JPA) |
| `@AllArgsConstructor` | Constructeur avec tous les champs |
| `@Builder` | Pattern Builder (`ColonneScore.builder().score1(3).build()`) |
| `@Data` | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor` |
| `@RequiredArgsConstructor` | Constructeur avec les champs `final` (pour injection Spring) |

### `@Builder.Default`
```java
@Builder.Default
private Integer totalNumbers = 0;
```

**Q : Pourquoi `@Builder.Default` ?**  
R : Sans cette annotation, quand on utilise le Builder, Lombok ignore l'initialisation `= 0` et met `null`. Avec `@Builder.Default`, la valeur par défaut est respectée même via le builder. À ne pas oublier pour les champs `@Column(nullable = false)`.

**Q : Pourquoi une entité JPA a besoin de `@NoArgsConstructor` ?**  
R : La spec JPA exige un constructeur sans argument pour que le provider (Hibernate) puisse instancier l'entité via réflexion lors des requêtes SELECT.

---

## H2 — Base de données en mémoire

```properties
spring.datasource.url=jdbc:h2:mem:yamdb;MODE=PostgreSQL
spring.jpa.hibernate.ddl-auto=update
```

**Q : Qu'est-ce que H2 ?**  
R : Une base de données relationnelle pure Java qui tourne en mémoire. Pratique pour les tests et les démos — pas besoin d'installer PostgreSQL. Le mode `MODE=PostgreSQL` émule la syntaxe PostgreSQL.

**Q : Que fait `ddl-auto=update` ?**  
R : Hibernate génère/met à jour automatiquement le schéma BDD à partir des entités au démarrage. En production on utiliserait `validate` (vérifie sans modifier) ou des migrations Flyway/Liquibase.
