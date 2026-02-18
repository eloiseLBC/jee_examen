package com.example.jee.examen.entity;

import com.example.jee.examen.enums.PartieStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parties {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_vainqueur")
    private Long idVainqueur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartieStatus status;
}
