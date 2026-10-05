package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
