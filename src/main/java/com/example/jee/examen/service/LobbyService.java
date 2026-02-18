package com.example.jee.examen.service;

import com.example.jee.examen.dto.LobbyReadyResponse;
import com.example.jee.examen.runtime.LobbyEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private static final long LOBBY_WAIT_MS = 60_000L;

    private final GameService gameService;

    private volatile LobbyEntry waitingPlayer;

    public synchronized LobbyReadyResponse ready(Long playerId) {
        long now = System.currentTimeMillis();
        cleanupIfExpired(now);

        if (waitingPlayer != null && waitingPlayer.getPlayerId().equals(playerId)) {
            int expiresIn = Math.max(0, (int) ((waitingPlayer.getExpiresAt() - now) / 1000));
            return LobbyReadyResponse.builder()
                    .matched(false)
                    .expiresInSec(expiresIn)
                    .build();
        }

        if (waitingPlayer == null) {
            waitingPlayer = LobbyEntry.builder()
                    .playerId(playerId)
                    .readyAt(now)
                    .expiresAt(now + LOBBY_WAIT_MS)
                    .build();
            return LobbyReadyResponse.builder()
                    .matched(false)
                    .expiresInSec(60)
                    .build();
        }

        Long opponentId = waitingPlayer.getPlayerId();
        waitingPlayer = null;
        Long gameId = gameService.createGame(opponentId, playerId);

        return LobbyReadyResponse.builder()
                .matched(true)
                .gameId(gameId)
                .build();
    }

    public synchronized void cancelReady(Long playerId) {
        long now = System.currentTimeMillis();
        cleanupIfExpired(now);
        if (waitingPlayer != null && waitingPlayer.getPlayerId().equals(playerId)) {
            waitingPlayer = null;
        }
    }

    private void cleanupIfExpired(long now) {
        if (waitingPlayer != null && waitingPlayer.getExpiresAt() < now) {
            waitingPlayer = null;
        }
    }
}
