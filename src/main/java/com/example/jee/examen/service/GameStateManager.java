package com.example.jee.examen.service;

import com.example.jee.examen.runtime.GameState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStateManager {

    private final Map<Long, GameState> states = new ConcurrentHashMap<>();

    public Optional<GameState> get(Long gameId) {
        return Optional.ofNullable(states.get(gameId));
    }

    public void put(Long gameId, GameState gameState) {
        states.put(gameId, gameState);
    }

    public void remove(Long gameId) {
        states.remove(gameId);
    }
}
