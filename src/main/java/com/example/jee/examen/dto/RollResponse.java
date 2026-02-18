package com.example.jee.examen.dto;

import com.example.jee.examen.enums.Category;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RollResponse {
    private int[] dice;
    private boolean[] locked;
    private int rollCount;
    private int rollsLeft;
    private long turnDeadlineAt;
    private Map<Category, Integer> possibleScores;
    private List<ScoreSheetDto> scores;
}
