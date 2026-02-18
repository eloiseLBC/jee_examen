package com.example.jee.examen.repository;

import com.example.jee.examen.entity.ColonneScore;
import com.example.jee.examen.enums.PartieStatus;
import com.example.jee.examen.service.HallOfFameRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ColonneScoreRepository extends JpaRepository<ColonneScore, Long> {
    Optional<ColonneScore> findByIdPartieAndIdJoueur(Long idPartie, Long idJoueur);
    List<ColonneScore> findByIdPartieOrderByIdJoueurAsc(Long idPartie);
    boolean existsByIdPartieAndIdJoueur(Long idPartie, Long idJoueur);

    @Query("""
        select new com.example.jee.examen.service.HallOfFameRow(c.idPartie, j.pseudo, c.scoreTotal)
        from ColonneScore c
        join Joueur j on j.id = c.idJoueur
        join Parties p on p.id = c.idPartie
        where p.status = :status
        order by c.scoreTotal desc
        """)
    List<HallOfFameRow> findTopByPartieStatus(PartieStatus status, Pageable pageable);
}
