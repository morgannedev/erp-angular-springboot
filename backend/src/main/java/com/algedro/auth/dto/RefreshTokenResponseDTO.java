package com.algedro.auth.dto;

public record RefreshTokenResponseDTO(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}