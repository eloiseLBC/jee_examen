package com.example.jee.examen.dto;

import com.example.jee.examen.enums.PartieStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GameResponse {
    private Long gameId;
    private PartieStatus status;
    private Long currentPlayerId;
    private List<Long> playerIds;
    private int[] dice;
    private boolean[] locked;
    private int rollCount;
    private long turnDeadlineAt;
    private List<ScoreSheetDto> scores;
    private Long winnerId;
}
