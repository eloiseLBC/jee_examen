package com.example.jee.examen.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreSheetDto {
    private Long playerId;
    private String pseudo;
    private Integer score1;
    private Integer score2;
    private Integer score3;
    private Integer score4;
    private Integer score5;
    private Integer score6;
    private Integer totalNumbers;
    private Integer totalNumbersBonus;
    private Integer scoreBrelan;
    private Integer scoreCarre;
    private Integer scoreFull;
    private Integer scorePetiteSuite;
    private Integer scoreGrandeSuite;
    private Integer scoreYam;
    private Integer scoreChance;
    private Integer scoreTotal;
}
