package com.example.jee.examen.service;

import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.repository.JoueurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final JoueurRepository joueurRepository;

    public Joueur currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Utilisateur non authentifié");
        }

        return joueurRepository.findByPseudo(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Utilisateur introuvable"));
    }
}
