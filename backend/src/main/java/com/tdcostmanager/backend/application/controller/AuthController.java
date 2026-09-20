package com.tdcostmanager.backend.application.controller;

import com.tdcostmanager.backend.application.dto.AuthResponse;
import com.tdcostmanager.backend.application.dto.LoginRequest;
import com.tdcostmanager.backend.application.dto.RegisterRequest;
import com.tdcostmanager.backend.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
