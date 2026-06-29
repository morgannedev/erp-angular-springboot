package com.algedro.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
    @NotBlank(message = "Refresh token es requerido")
    String refreshToken
) {}