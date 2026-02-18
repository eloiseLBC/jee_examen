package com.example.jee.examen.config;

import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.repository.JoueurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final JoueurRepository joueurRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            seed("alice", "password");
            seed("bob", "password");
            seed("charlie", "password");
        };
    }

    private void seed(String pseudo, String rawPassword) {
        if (!joueurRepository.existsByPseudo(pseudo)) {
            joueurRepository.save(Joueur.builder()
                    .pseudo(pseudo)
                    .mdp(passwordEncoder.encode(rawPassword))
                    .build());
        }
    }
}
