package com.example.jee.examen.repository;

import com.example.jee.examen.entity.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {
    Optional<Joueur> findByPseudo(String pseudo);
    boolean existsByPseudo(String pseudo);
}
