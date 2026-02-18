package com.example.jee.examen.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyReadyResponse {
    private boolean matched;
    private Long gameId;
    private Integer expiresInSec;
}
