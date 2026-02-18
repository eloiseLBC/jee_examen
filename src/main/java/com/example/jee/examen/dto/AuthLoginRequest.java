package com.example.jee.examen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequest {
    @NotBlank
    private String pseudo;

    @NotBlank
    private String password;
}
