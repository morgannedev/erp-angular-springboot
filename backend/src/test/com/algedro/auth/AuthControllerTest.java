package com.algedro.auth;

import com.algedro.config.SecurityConfig;
import com.algedro.exception.GlobalExceptionHandler;
import com.algedro.security.JwtFilter;
import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.algedro.security.UserDetailsServiceImpl;
import com.algedro.usuario.Rol;
import com.algedro.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void testLogin_200() throws Exception {
        AuthResult result = new AuthResult("jwt-token", "refresh-token", Rol.ADMIN, 1L, "admin", 28800);
        when(authService.login("admin", "admin123")).thenReturn(result);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                // ✅ LoginResponseDTO tiene: token, refreshToken, rol, empleadoId, username, expiraEn
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.empleadoId").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.expiraEn").value(28800));
    }

    @Test
    void testLogin_401_credencialesErroneas() throws Exception {
        when(authService.login("admin", "wrong"))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout_200_conToken() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setRol(Rol.ADMIN);

        when(jwtUtils.extractUsername(anyString())).thenReturn("admin");
        when(jwtUtils.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(usuario);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer jwt-token")
                        .header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testLogout_401_sinToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMe_200_conTokenValido() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setRol(Rol.ADMIN);

        when(jwtUtils.extractUsername(anyString())).thenReturn("admin");
        when(jwtUtils.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(usuario);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void testMe_401_sinToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMe_401_conTokenExpirado() throws Exception {
        when(jwtUtils.extractUsername(anyString())).thenReturn("admin");
        when(jwtUtils.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(false);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefresh_200() throws Exception {
        AuthResult result = new AuthResult("new-jwt-token", "new-refresh-token", Rol.ADMIN, 1L, "admin", 28800);
        when(refreshTokenService.refreshAccessToken("valid-refresh-token")).thenReturn(result);

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                // ✅ RefreshTokenResponseDTO tiene: accessToken, refreshToken, expiresIn
                .andExpect(jsonPath("$.accessToken").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(28800));
    }

    @Test
    void testRefresh_401_tokenInvalido() throws Exception {
        when(refreshTokenService.refreshAccessToken("invalid-refresh-token"))
                .thenThrow(new BadCredentialsException("Refresh token inválido"));

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefresh_400_tokenVacio() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRefresh_400_sinToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}