package com.example.jee.examen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String pseudo;

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;
}
