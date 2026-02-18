package com.example.jee.examen.controller;

import com.example.jee.examen.dto.GameResponse;
import com.example.jee.examen.dto.LockRequest;
import com.example.jee.examen.dto.RollResponse;
import com.example.jee.examen.dto.ScoreRequest;
import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.service.AuthenticatedUserService;
import com.example.jee.examen.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/{gameId}")
    public GameResponse getGame(@PathVariable Long gameId) {
        Joueur joueur = authenticatedUserService.currentUser();
        return gameService.getGame(gameId, joueur.getId());
    }

    @PostMapping("/{gameId}/roll")
    public RollResponse roll(@PathVariable Long gameId) {
        Joueur joueur = authenticatedUserService.currentUser();
        return gameService.roll(gameId, joueur.getId());
    }

    @PostMapping("/{gameId}/lock")
    public RollResponse lockAndRoll(@PathVariable Long gameId, @Valid @RequestBody LockRequest request) {
        Joueur joueur = authenticatedUserService.currentUser();
        return gameService.lockAndRoll(gameId, joueur.getId(), request.getLockedIndexes());
    }

    @PostMapping("/{gameId}/score")
    public GameResponse score(@PathVariable Long gameId, @Valid @RequestBody ScoreRequest request) {
        Joueur joueur = authenticatedUserService.currentUser();
        return gameService.score(gameId, joueur.getId(), request.getCategory());
    }
}
