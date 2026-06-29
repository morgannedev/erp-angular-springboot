package com.algedro.auth.dto;

import com.algedro.auth.AuthResult;

public record LoginResponseDTO(
        String token,
        String refreshToken,
        String rol,
        Long empleadoId,
        String username,
        long expiraEn
) {
    public static LoginResponseDTO from(AuthResult result) {
        return new LoginResponseDTO(
                result.token(),
                result.refreshToken(),  // ← Usar directamente el refreshToken del result
                result.rol().name(),
                result.empleadoId(),
                result.username(),
                result.expiraEn()
        );
    }
}