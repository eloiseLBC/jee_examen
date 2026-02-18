package com.example.jee.examen.controller;

import com.example.jee.examen.dto.AuthLoginRequest;
import com.example.jee.examen.dto.AuthRegisterRequest;
import com.example.jee.examen.dto.AuthResponse;
import com.example.jee.examen.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AuthRegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        String token = authService.login(request);
        return new AuthResponse(token);
    }
}
