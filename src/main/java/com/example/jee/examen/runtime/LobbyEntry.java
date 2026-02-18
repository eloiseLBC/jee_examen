package com.example.jee.examen.runtime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyEntry {
    private Long playerId;
    private long readyAt;
    private long expiresAt;
}
