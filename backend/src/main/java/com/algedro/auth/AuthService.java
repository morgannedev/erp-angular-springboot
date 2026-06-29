package com.algedro.auth;

import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.algedro.usuario.Usuario;
import com.algedro.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;  // ← Añadir RefreshTokenService
    private final int maxFailedAttempts;
    private final int lockDurationMinutes;
    private final long expirationSeconds;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            TokenBlacklistService tokenBlacklistService,
            RefreshTokenService refreshTokenService,  // ← Añadir al constructor
            @Value("${algedro.jwt.max-failed-attempts}") int maxFailedAttempts,
            @Value("${algedro.jwt.lock-duration-minutes}") int lockDurationMinutes,
            @Value("${algedro.jwt.expiration-ms}") long expirationMs
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;  // ← Inicializar
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
        this.expirationSeconds = expirationMs / 1000;
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class})
    public AuthResult login(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!usuario.isEnabled()) {
            simulateDelay();
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            simulateDelay();
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            short nextAttempts = (short) (usuario.getIntentosFallidos() + 1);
            usuario.setIntentosFallidos(nextAttempts);

            if (nextAttempts >= maxFailedAttempts) {
                usuario.setBloqueadoHasta(OffsetDateTime.now().plusMinutes(lockDurationMinutes));
                usuarioRepository.save(usuario);
                simulateDelay();
                throw new BadCredentialsException("Credenciales incorrectas");
            }
            usuarioRepository.save(usuario);
            simulateDelay();
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        usuario.setIntentosFallidos((short) 0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(OffsetDateTime.now());

        usuarioRepository.save(usuario);

        String token = jwtUtils.generateToken(usuario);

        // Crear refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario.getId(), usuario.getUsername());

        logger.info("Login exitoso para usuario: {}, refresh token creado: {}",
                username, refreshToken.getToken());

        return new AuthResult(
                token,
                refreshToken.getToken(),  // ← Incluir refresh token
                usuario.getRol(),
                usuario.getId(),
                usuario.getUsername(),
                expirationSeconds
        );
    }

    @Transactional
    public void logout(String token, String refreshToken) {
        // Revocar access token
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            long expirationTime = jwtUtils.getExpirationTime(token);
            tokenBlacklistService.blacklistToken(token, expirationTime);
        }

        // Revocar refresh token si existe
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }

        SecurityContextHolder.clearContext();
    }

    private void simulateDelay() {
        try {
            Thread.sleep(50 + (int)(Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}