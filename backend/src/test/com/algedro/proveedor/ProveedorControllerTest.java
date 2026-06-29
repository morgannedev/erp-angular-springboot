package com.algedro.proveedor;

import com.algedro.config.SecurityConfig;
import com.algedro.exception.ConflictoException;
import com.algedro.security.JwtFilter;
import com.algedro.security.JwtUtils;
import com.algedro.proveedor.controller.ProveedorController;
import com.algedro.proveedor.dto.ProveedorRequestDTO;
import com.algedro.proveedor.dto.ProveedorResponseDTO;
import com.algedro.proveedor.service.ProveedorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.algedro.security.TokenBlacklistService;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fase 3 — ProveedorControllerTest
 *
 * Fuentes de verdad:
 *   - openapi.yaml → GET  /proveedores x-roles: [ADMIN, EMPLEADO] → 200
 *                  → POST /proveedores x-roles: [ADMIN]            → 403 para EMPLEADO
 *   - technical-design.md → @WebMvcTest + MockMvc para tests de slice de Controller
 *
 * Seguridad:
 *   - @WithMockUser(roles = "EMPLEADO") simula el JWT validado por JwtFilter
 *   - @PreAuthorize en el Controller define el acceso real
 */
@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
@DisplayName("ProveedorController — Fase 3")
class ProveedorControllerTest {

    private static final String BASE_URL = "/proveedores";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProveedorService proveedorService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;   // interfaz — satisface el constructor de JwtFilter

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    // ── SIN @MockBean JwtFilter ──────────────────────────────────────
    // JwtFilter es un OncePerRequestFilter real que, sin token JWT en
    // la cabecera, llama a chain.doFilter() y deja pasar.
    // @WithMockUser inyecta la autenticación en el SecurityContext
    // directamente, sin pasar por el filtro JWT.
    // ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "maria", roles = "EMPLEADO")
    @DisplayName("GET /proveedores con rol EMPLEADO → 200 con lista paginada")
    void testGet_rolEmpleado_200() throws Exception {
        ProveedorResponseDTO dto = proveedorResponseDTO();
        Page<ProveedorResponseDTO> page = new PageImpl<>(List.of(dto));
        given(proveedorService.listar(any(Pageable.class), any(), any()))
                .willReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nif").value("B12345678"))
                .andExpect(jsonPath("$.contenido[0].nombre")
                        .value("Distribuciones Químicas del Sur S.L."));
    }

    @Test
    @WithMockUser(username = "maria", roles = "EMPLEADO")
    @DisplayName("POST /proveedores con rol EMPLEADO → 403 Forbidden")
    void testPost_rolEmpleado_403() throws Exception {
        ProveedorRequestDTO requestDTO = new ProveedorRequestDTO(
                "Proveedor Nuevo S.L.", "C99999999",
                null, null, null, null, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());

        then(proveedorService).should(never()).crear(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("POST /proveedores con rol ADMIN → 201 Created")
    void testPost_rolAdmin_201() throws Exception {
        ProveedorRequestDTO requestDTO = new ProveedorRequestDTO(
                "Proveedor Nuevo S.L.", "C99999999",
                "Contacto Test", "600000000", "test@proveedor.es",
                null, null, null
        );
        given(proveedorService.crear(any())).willReturn(proveedorResponseDTO());

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nif").value("B12345678"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("POST /proveedores con NIF duplicado → 409 Conflict")
    void testPost_nifDuplicado_409() throws Exception {
        ProveedorRequestDTO requestDTO = new ProveedorRequestDTO(
                "Duplicado S.L.", "B12345678",
                null, null, null, null, null, null
        );
        given(proveedorService.crear(any()))
                .willThrow(new ConflictoException("NIF B12345678 ya está registrado"));

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    private ProveedorResponseDTO proveedorResponseDTO() {
        return new ProveedorResponseDTO(
                1L, "Distribuciones Químicas del Sur S.L.", "B12345678",
                "Pedro Martínez", "954111222", "pedidos@dqsur.es",
                "Calle Ejemplo, 1", "Sevilla", true, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
