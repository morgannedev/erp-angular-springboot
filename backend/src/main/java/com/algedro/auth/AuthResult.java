package com.algedro.auth;

import com.algedro.usuario.Rol;

public record AuthResult(
        String token,
        String refreshToken,  // ← Añadir refresh token
        Rol rol,
        Long empleadoId,
        String username,
        long expiraEn
) {
    // Constructor sin refresh token para compatibilidad (si es necesario)
    public AuthResult(String token, Rol rol, Long empleadoId, String username, long expiraEn) {
        this(token, null, rol, empleadoId, username, expiraEn);
    }
}