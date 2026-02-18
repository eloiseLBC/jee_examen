package com.example.jee.examen.service;

import com.example.jee.examen.dto.LobbyReadyResponse;
import com.example.jee.examen.runtime.LobbyEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private static final long LOBBY_WAIT_MS = 60_000L;

    private final GameService gameService;

    private volatile LobbyEntry waitingPlayer;

    /** Stocke les gameId pour les joueurs matchés qui n'ont pas encore récupéré leur résultat */
    private final Map<Long, Long> pendingGameIds = new ConcurrentHashMap<>();

    public synchronized LobbyReadyResponse ready(Long playerId) {
        // Joueur A a été matché pendant qu'il attendait → il récupère son gameId au prochain poll
        Long pendingGameId = pendingGameIds.remove(playerId);
        if (pendingGameId != null) {
            return LobbyReadyResponse.builder()
                    .matched(true)
                    .gameId(pendingGameId)
                    .build();
        }

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

        // Stocker le gameId pour que le Joueur A (qui poll) puisse le récupérer
        pendingGameIds.put(opponentId, gameId);

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
