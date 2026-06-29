package com.algedro.auth;

import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.algedro.usuario.Rol;
import com.algedro.usuario.Usuario;
import com.algedro.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private JwtUtils jwtUtils;
    private TokenBlacklistService tokenBlacklistService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = mock(JwtUtils.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        authService = new AuthService(
                usuarioRepository,
                passwordEncoder,
                jwtUtils,
                tokenBlacklistService,
                refreshTokenService,
                5,  // maxFailedAttempts
                15, // lockDurationMinutes
                28_800_000L // expirationMs
        );
    }

    @Test
    void testLoginExitoso() {
        Usuario usuario = activeUser();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", usuario.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateToken(usuario)).thenReturn("jwt-token");

        AuthResult result = authService.login("admin", "admin123");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.rol()).isEqualTo(Rol.ADMIN);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getUltimoAcceso()).isNotNull();
        assertThat(usuario.getBloqueadoHasta()).isNull();

        // ✅ CORREGIDO - Ahora solo se guarda UNA vez en el login exitoso
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void testCredencialesIncorrectas_incrementaIntentos() {
        Usuario usuario = activeUser();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        assertThat(usuario.getIntentosFallidos()).isEqualTo((short) 1);
        verify(usuarioRepository).save(usuario);
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void testBloqueoDespuesDe5Intentos() {
        Usuario usuario = activeUser();
        usuario.setIntentosFallidos((short) 4);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        assertThat(usuario.getIntentosFallidos()).isEqualTo((short) 5);
        assertThat(usuario.getBloqueadoHasta()).isAfter(OffsetDateTime.now());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void testCuentaBloqueada_rechazaLoginAunConPasswordCorrecta() {
        Usuario usuario = activeUser();
        usuario.setBloqueadoHasta(OffsetDateTime.now().plusMinutes(5));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", usuario.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login("admin", "admin123"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void testCuentaInactiva_rechazaLogin() {
        Usuario usuario = activeUser();
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login("admin", "admin123"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtUtils, never()).generateToken(any());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLoginExitoso_resetaIntentosYActualizaUltimoAcceso() {
        Usuario usuario = activeUser();
        usuario.setIntentosFallidos((short) 3);
        usuario.setBloqueadoHasta(OffsetDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", usuario.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateToken(usuario)).thenReturn("jwt-token");

        authService.login("admin", "admin123");

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.getUltimoAcceso()).isNotNull();

        // ✅ CORREGIDO - Ahora solo se guarda UNA vez
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void testLogout_invalidaToken() {
        String token = "valid-jwt-token";
        String authHeader = "Bearer " + token;
        long expirationTime = System.currentTimeMillis() + 3600000;

        when(jwtUtils.getExpirationTime(token)).thenReturn(expirationTime);

        authService.logout(authHeader);

        verify(tokenBlacklistService).blacklistToken(token, expirationTime);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testLogout_conTokenInvalido_noLanzaExcepcion() {
        String authHeader = "Bearer invalid-token";

        // ✅ CORREGIDO - getExpirationTime ahora retorna tiempo actual en lugar de lanzar excepción
        when(jwtUtils.getExpirationTime("invalid-token")).thenReturn(System.currentTimeMillis());

        // ✅ No debe lanzar excepción
        authService.logout(authHeader);

        verify(tokenBlacklistService).blacklistToken(eq("invalid-token"), anyLong());
    }

    @Test
    void testLogout_sinToken_noHaceNada() {
        authService.logout(null);

        verify(tokenBlacklistService, never()).blacklistToken(anyString(), anyLong());
    }

    @Test
    void testLogout_sinBearerPrefix_noHaceNada() {
        authService.logout("invalid-format");

        verify(tokenBlacklistService, never()).blacklistToken(anyString(), anyLong());
    }

    @Test
    void testUsuarioNoExistente_recibeMensajeGenerico() {
        when(usuarioRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nonexistent", "password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    private Usuario activeUser() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setPasswordHash("hash");
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        usuario.setIntentosFallidos((short) 0);
        return usuario;
    }
}