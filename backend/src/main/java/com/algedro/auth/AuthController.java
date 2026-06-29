package com.algedro.auth;

import com.algedro.auth.dto.*;
import com.algedro.usuario.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResult result = authService.login(request.username(), request.password());
        return ResponseEntity.ok(LoginResponseDTO.from(result));  // ← Pasar solo result
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        logger.info("Solicitud de refresh token recibida");
        try {
            AuthResult result = refreshTokenService.refreshAccessToken(request.refreshToken());
            logger.info("Refresh token exitoso para usuario: {}", result.username());
            return ResponseEntity.ok(new RefreshTokenResponseDTO(
                    result.token(),
                    result.refreshToken(),
                    result.expiraEn()
            ));
        } catch (BadCredentialsException e) {
            logger.error("Error en refresh token: {}", e.getMessage());
            throw e;  // Deja que el exception handler maneje el 401
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        String token = request.getHeader("Authorization");
        authService.logout(token, refreshToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new MeResponseDTO(usuario.getId(), usuario.getUsername(), usuario.getRol().name()));
    }
}