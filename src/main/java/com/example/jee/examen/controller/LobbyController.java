package com.example.jee.examen.controller;

import com.example.jee.examen.dto.LobbyReadyResponse;
import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.service.AuthenticatedUserService;
import com.example.jee.examen.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/ready")
    public LobbyReadyResponse ready() {
        Joueur joueur = authenticatedUserService.currentUser();
        return lobbyService.ready(joueur.getId());
    }

    @DeleteMapping("/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReady() {
        Joueur joueur = authenticatedUserService.currentUser();
        lobbyService.cancelReady(joueur.getId());
    }
}
