package com.tdcostmanager.backend.application.dto;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn
) {}
