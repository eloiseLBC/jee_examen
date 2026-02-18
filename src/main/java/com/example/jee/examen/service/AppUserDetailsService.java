package com.example.jee.examen.service;

import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.repository.JoueurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final JoueurRepository joueurRepository;

    @Override
    public UserDetails loadUserByUsername(String pseudo) throws UsernameNotFoundException {
        Joueur joueur = joueurRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

        return User.withUsername(joueur.getPseudo())
                .password(joueur.getMdp())
                .roles("USER")
                .build();
    }
}
