package com.example.jee.examen.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "joueur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Joueur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pseudo;

    @Column(nullable = false)
    private String mdp;
}
