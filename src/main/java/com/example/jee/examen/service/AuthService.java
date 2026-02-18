package com.example.jee.examen.service;

import com.example.jee.examen.dto.AuthLoginRequest;
import com.example.jee.examen.dto.AuthRegisterRequest;
import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.repository.JoueurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JoueurRepository joueurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(AuthRegisterRequest request) {
        if (joueurRepository.existsByPseudo(request.getPseudo())) {
            throw new ResponseStatusException(BAD_REQUEST, "Pseudo déjà utilisé");
        }

        Joueur joueur = Joueur.builder()
                .pseudo(request.getPseudo())
                .mdp(passwordEncoder.encode(request.getPassword()))
                .build();
        joueurRepository.save(joueur);
    }

    public String login(AuthLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPseudo(), request.getPassword())
        );
        return jwtService.generateToken(request.getPseudo());
    }
}
