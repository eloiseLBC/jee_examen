package com.example.jee.examen.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "colonneScore")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColonneScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_partie", nullable = false)
    private Long idPartie;

    @Column(name = "id_joueur", nullable = false)
    private Long idJoueur;

    @Column(name = "score_1")
    private Integer score1;

    @Column(name = "score_2")
    private Integer score2;

    @Column(name = "score_3")
    private Integer score3;

    @Column(name = "score_4")
    private Integer score4;

    @Column(name = "score_5")
    private Integer score5;

    @Column(name = "score_6")
    private Integer score6;

    @Column(name = "total_numbers", nullable = false)
    @Builder.Default
    private Integer totalNumbers = 0;

    @Column(name = "total_numbers_bonus", nullable = false)
    @Builder.Default
    private Integer totalNumbersBonus = 0;

    @Column(name = "score_brelan")
    private Integer scoreBrelan;

    @Column(name = "score_carre")
    private Integer scoreCarre;

    @Column(name = "score_full")
    private Integer scoreFull;

    @Column(name = "score_petite_suite")
    private Integer scorePetiteSuite;

    @Column(name = "score_grande_suite")
    private Integer scoreGrandeSuite;

    @Column(name = "score_yam")
    private Integer scoreYam;

    @Column(name = "score_chance")
    private Integer scoreChance;

    @Column(name = "score_total", nullable = false)
    @Builder.Default
    private Integer scoreTotal = 0;
}
