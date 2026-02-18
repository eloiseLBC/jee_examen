package com.example.jee.examen.runtime;

import com.example.jee.examen.enums.RuntimeGameStatus;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameState {
    private Long partieId;
    private List<Long> playerIds;
    private Long currentPlayerId;
    private int[] dice;
    private boolean[] locked;
    private int rollCount;
    private long turnStartedAt;
    private long turnDeadlineAt;
    private Map<Long, Integer> extraYamCount;
    private RuntimeGameStatus status;
}
