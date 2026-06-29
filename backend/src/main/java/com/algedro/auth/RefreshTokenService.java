package com.algedro.auth;

import com.algedro.security.JwtUtils;
import com.algedro.usuario.Usuario;
import com.algedro.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtils jwtUtils;

    @Value("${algedro.jwt.refresh-expiration-ms:604800000}")
    private long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(Long usuarioId, String username) {
        logger.info("Creando refresh token para usuario: {}", username);

        // Limpiar tokens expirados
        refreshTokenRepository.deleteExpiredTokens(Instant.now());

        // Buscar si ya existe un token activo para este usuario
        Optional<RefreshToken> existingToken = refreshTokenRepository
                .findByUsuarioIdAndRevokedFalse(usuarioId);

        if (existingToken.isPresent()) {
            RefreshToken token = existingToken.get();

            // Si el token existente no ha expirado, lo devolvemos
            if (token.getExpiryDate().isAfter(Instant.now())) {
                logger.info("Usuario {} ya tiene un refresh token activo válido hasta: {}",
                        username, token.getExpiryDate());
                return token;
            } else {
                // Si expiró, lo revocamos y creamos uno nuevo
                logger.info("Refresh token existente para usuario {} ha expirado, revocando", username);
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
        }

        // Crear nuevo refresh token
        String tokenValue = UUID.randomUUID().toString();
        logger.debug("Nuevo token generado: {}", tokenValue);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .usuarioId(usuarioId)
                .username(username)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token creado exitosamente para usuario: {}, expira en: {} ms",
                username, refreshTokenDurationMs);

        return saved;
    }

    @Transactional
    public RefreshToken verifyAndGetRefreshToken(String token) {
        logger.info("Verificando refresh token: {}", token);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> {
                    logger.error("Refresh token no encontrado o revocado: {}", token);
                    return new BadCredentialsException("Refresh token inválido");
                });

        logger.debug("Refresh token encontrado. Usuario: {}, Expira: {}, Revocado: {}",
                refreshToken.getUsername(), refreshToken.getExpiryDate(), refreshToken.isRevoked());

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            logger.error("Refresh token expirado para usuario: {}", refreshToken.getUsername());
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadCredentialsException("Refresh token expirado");
        }

        logger.info("Refresh token verificado exitosamente para usuario: {}", refreshToken.getUsername());
        return refreshToken;
    }

    @Transactional
    public AuthResult refreshAccessToken(String refreshTokenValue) {
        logger.info("Procesando solicitud de refresh token");

        // Verificar el refresh token actual
        RefreshToken oldRefreshToken = verifyAndGetRefreshToken(refreshTokenValue);

        // Revocar el refresh token usado (rotación)
        oldRefreshToken.setRevoked(true);
        refreshTokenRepository.save(oldRefreshToken);
        logger.info("Refresh token antiguo revocado para usuario: {}", oldRefreshToken.getUsername());

        // Obtener el usuario
        Usuario usuario = usuarioRepository.findById(oldRefreshToken.getUsuarioId())
                .orElseThrow(() -> {
                    logger.error("Usuario no encontrado para refresh token: {}", oldRefreshToken.getUsuarioId());
                    return new BadCredentialsException("Usuario no encontrado");
                });

        if (!usuario.isEnabled()) {
            logger.error("Usuario deshabilitado: {}", usuario.getUsername());
            throw new BadCredentialsException("Usuario deshabilitado");
        }

        logger.info("Refresh token válido para usuario: {}", usuario.getUsername());

        // Actualizar último acceso
        usuario.setUltimoAcceso(java.time.OffsetDateTime.now());
        usuarioRepository.save(usuario);

        // Generar nuevo access token
        String newAccessToken = jwtUtils.generateToken(usuario);
        logger.debug("Nuevo access token generado para usuario: {}", usuario.getUsername());

        // Crear nuevo refresh token (rotación)
        RefreshToken newRefreshToken = createRefreshToken(usuario.getId(), usuario.getUsername());

        return new AuthResult(
                newAccessToken,
                newRefreshToken.getToken(),
                usuario.getRol(),
                usuario.getId(),
                usuario.getUsername(),
                refreshTokenDurationMs / 1000
        );
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        logger.info("Revocando refresh token: {}", token);
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                    logger.info("Refresh token revocado para usuario: {}", refreshToken.getUsername());
                });
    }

    @Transactional
    public void revokeAllUserRefreshTokens(Long usuarioId) {
        logger.info("Revocando todos los refresh tokens para usuario ID: {}", usuarioId);
        refreshTokenRepository.revokeAllUserTokens(usuarioId);
        logger.debug("Todos los refresh tokens revocados para usuario ID: {}", usuarioId);
    }
}