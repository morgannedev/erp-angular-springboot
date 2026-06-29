package com.algedro.empleado;

import com.algedro.empleado.controller.EmpleadoController;
import com.algedro.empleado.dto.EmpleadoCreateRequestDTO;
import com.algedro.empleado.dto.EmpleadoResponseDTO;
import com.algedro.empleado.dto.EmpleadoUpdateRequestDTO;
import com.algedro.empleado.enums.RolEmpleado;
import com.algedro.empleado.service.EmpleadoService;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.DuplicateResourceException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de slice (@WebMvcTest) para EmpleadoController.
 *
 * Ciclo TDD — Fase 2: estos tests deben estar en RED antes de que exista
 * la implementación de EmpleadoController. Solo se escribe el controller
 * después de que todos estos tests fallen correctamente por ausencia de
 * implementación, no por errores de compilación en los tests.
 *
 * Casos cubiertos (según openapi.yaml y Contexto_e_instrucciones.md §2):
 *  - GET  /empleados            → 200 ADMIN, 403 EMPLEADO
 *  - GET  /empleados/{id}       → 200 ADMIN, 403 EMPLEADO, 404
 *  - POST /empleados            → 201 ADMIN, 400 (validación), 403 EMPLEADO, 409 (duplicado)
 *  - PUT  /empleados/{id}       → 200 ADMIN, 400 (validación), 403 EMPLEADO, 404, 409 (duplicado)
 *  - PATCH /empleados/{id}/cuenta → 200 ADMIN, 403 EMPLEADO, 404
 *  - PATCH /empleados/{id}/rol    → 200 ADMIN, 403 EMPLEADO, 404
 *  - DELETE /empleados/{id}     → 204 ADMIN (sin ventas), 403 EMPLEADO, 409 (con ventas)
 */

@DisplayName("EmpleadoController — Tests de slice WebMvcTest")
@WebMvcTest(EmpleadoController.class)
@Import(EmpleadoControllerTest.TestSecurityConfig.class)
class EmpleadoControllerTest {

    private static final String BASE_URL = "/empleados";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmpleadoService empleadoService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void resetMocks() {
        reset(empleadoService, jwtUtils);
    }

    /*REVISAR*/
    @TestConfiguration
    @EnableMethodSecurity // Permite que @PreAuthorize("hasRole('ADMIN')") funcione en los tests
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable()) // Deshabilita CSRF para facilitar las pruebas API REST
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()); // Delega el control a las anotaciones @PreAuthorize
            return http.build();
        }
    }

    // ─────────────────────────────────────────────
    // Helpers de fixtures
    // ─────────────────────────────────────────────

    private EmpleadoResponseDTO empleadoResponseDTOEjemplo() {
        EmpleadoResponseDTO dto = new EmpleadoResponseDTO();
        dto.setId(1L);
        dto.setUsuarioId(10L);
        dto.setUsername("carlos");
        dto.setRol(RolEmpleado.EMPLEADO);
        dto.setCuentaActiva(true);
        dto.setNombre("Carlos");
        dto.setApellidos("García López");
        dto.setDni("12345678A");
        dto.setTelefono("600123456");
        dto.setEmail("carlos@algedro.com");
        dto.setCargo("Dependiente");
        dto.setSalario(null); // omitido para EMPLEADO; visible solo para ADMIN
        dto.setFechaContratacion(LocalDate.of(2023, 1, 15));
        dto.setFechaBaja(null);
        dto.setNotas(null);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private EmpleadoCreateRequestDTO createRequestValido() {
        EmpleadoCreateRequestDTO req = new EmpleadoCreateRequestDTO();
        req.setUsername("nuevo");
        req.setPassword("pass1234");
        req.setRol(RolEmpleado.EMPLEADO);
        req.setNombre("Nuevo");
        req.setApellidos("Empleado Test");
        req.setCargo("Dependiente");
        req.setFechaContratacion(LocalDate.of(2024, 6, 1));
        return req;
    }

    private EmpleadoUpdateRequestDTO updateRequestValido() {
        EmpleadoUpdateRequestDTO req = new EmpleadoUpdateRequestDTO();
        req.setNombre("Carlos Actualizado");
        req.setApellidos("García López");
        req.setCargo("Encargado");
        req.setSalario(1800.00);
        return req;
    }

    // ═══════════════════════════════════════════════════════════
    // GET /empleados
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /empleados — Listar empleados")
    class ListarEmpleados {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN → devuelve 200 con página de empleados")
        void testGetEmpleados_rolAdmin_200() throws Exception {
            // GIVEN
            Page<EmpleadoResponseDTO> pagina = new PageImpl<>(
                    List.of(empleadoResponseDTOEjemplo()),
                    PageRequest.of(0, 25),
                    1L
            );
            when(empleadoService.buscarConFiltros(any(), any(), any(Pageable.class))).thenReturn(pagina);
            // WHEN / THEN
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].username").value("carlos"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(empleadoService, times(1)).buscarConFiltros(any(), any(), any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testGetEmpleados_rolEmpleado_403() throws Exception {
            // WHEN / THEN
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @DisplayName("Sin autenticación → devuelve 401 Unauthorized")
        void testGetEmpleados_sinToken_401() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Filtro por nombre parcial → llega el parámetro q al servicio")
        void testGetEmpleados_filtroPorNombre_200() throws Exception {
            // GIVEN
            Page<EmpleadoResponseDTO> pagina = new PageImpl<>(
                    List.of(empleadoResponseDTOEjemplo()),
                    PageRequest.of(0, 25),
                    1L
            );
            when(empleadoService.buscarConFiltros(any(), any(), any(Pageable.class))).thenReturn(pagina);

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL)
                            .param("q", "Carlos")
                            .param("page", "0")
                            .param("size", "25")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /empleados/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /empleados/{id} — Obtener empleado por ID")
    class ObtenerEmpleadoPorId {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, ID existente → devuelve 200 con el empleado")
        void testGetEmpleado_rolAdmin_200() throws Exception {
            // GIVEN
            when(empleadoService.getEmpleadoPorId(1L)).thenReturn(empleadoResponseDTOEjemplo());
            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("carlos"))
                    .andExpect(jsonPath("$.nombre").value("Carlos"))
                    .andExpect(jsonPath("$.cuentaActiva").value(true));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testGetEmpleado_rolEmpleado_403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ID inexistente → devuelve 404 Not Found")
        void testGetEmpleado_noExiste_404() throws Exception {
            // GIVEN
            when(empleadoService.getEmpleadoPorId(999L))
                    .thenThrow(new ResourceNotFoundException("Empleado no encontrado con id: 999"));

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.mensaje").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POST /empleados
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /empleados — Crear empleado")
    class CrearEmpleado {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, datos válidos → devuelve 201 con el empleado creado")
        void testCrearEmpleado_rolAdmin_201() throws Exception {
            // GIVEN
            EmpleadoCreateRequestDTO request = createRequestValido();
            EmpleadoResponseDTO response = empleadoResponseDTOEjemplo();
            response.setUsername("nuevo");

            when(empleadoService.crearEmpleado(any(EmpleadoCreateRequestDTO.class))).thenReturn(response);
            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value("nuevo"));

            verify(empleadoService, times(1)).crearEmpleado(any(EmpleadoCreateRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testCrearEmpleado_rolEmpleado_403() throws Exception {
            // GIVEN
            EmpleadoCreateRequestDTO request = createRequestValido();

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Campos obligatorios ausentes → devuelve 400 Bad Request")
        void testCrearEmpleado_camposObligatoriosAusentes_400() throws Exception {
            // GIVEN — request sin username, nombre, apellidos ni cargo (campos obligatorios)
            EmpleadoCreateRequestDTO requestInvalido = new EmpleadoCreateRequestDTO();
            requestInvalido.setPassword("pass1234");
            // username, nombre, apellidos, cargo, fechaContratacion ausentes

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Username o DNI duplicado → devuelve 409 Conflict")
        void testCrearEmpleado_usernameDuplicado_409() throws Exception {
            // GIVEN
            EmpleadoCreateRequestDTO request = createRequestValido();
            when(empleadoService.crearEmpleado(any(EmpleadoCreateRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("El username 'nuevo' ya existe"));

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensaje").exists());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol inválido en el request → devuelve 400 Bad Request")
        void testCrearEmpleado_rolInvalido_400() throws Exception {
            // GIVEN
            EmpleadoCreateRequestDTO request = createRequestValido();
            java.util.Map<String, Object> requestMap = objectMapper.convertValue(request, java.util.Map.class);
            requestMap.put("rol", "SUPERADMIN"); // valor no permitido por el enum

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestMap)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /empleados/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /empleados/{id} — Actualizar empleado")
    class ActualizarEmpleado {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, datos válidos → devuelve 200 con el empleado actualizado")
        void testActualizarEmpleado_rolAdmin_200() throws Exception {
            // GIVEN
            EmpleadoUpdateRequestDTO request = updateRequestValido();
            EmpleadoResponseDTO response = empleadoResponseDTOEjemplo();
            response.setNombre("Carlos Actualizado");
            response.setCargo("Encargado");

            when(empleadoService.actualizarEmpleado(eq(1L), any(EmpleadoUpdateRequestDTO.class)))
                    .thenReturn(response);

            // WHEN / THEN
            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Carlos Actualizado"))
                    .andExpect(jsonPath("$.cargo").value("Encargado"));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testActualizarEmpleado_rolEmpleado_403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequestValido())))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ID inexistente → devuelve 404 Not Found")
        void testActualizarEmpleado_noExiste_404() throws Exception {
            // GIVEN
            when(empleadoService.actualizarEmpleado(eq(999L), any(EmpleadoUpdateRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Empleado no encontrado con id: 999"));

            // WHEN / THEN
            mockMvc.perform(put(BASE_URL + "/999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequestValido())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DNI o email duplicado en actualización → devuelve 409 Conflict")
        void testActualizarEmpleado_dniDuplicado_409() throws Exception {
            // GIVEN
            EmpleadoUpdateRequestDTO request = updateRequestValido();
            request.setDni("87654321B"); // DNI que ya pertenece a otro empleado

            when(empleadoService.actualizarEmpleado(eq(1L), any(EmpleadoUpdateRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("El DNI '87654321B' ya está en uso"));

            // WHEN / THEN
            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PATCH /empleados/{id}/cuenta
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /empleados/{id}/cuenta — Activar/desactivar cuenta")
    class GestionarCuenta {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, desactivar cuenta existente → devuelve 200")
        void testDesactivarCuenta_rolAdmin_200() throws Exception {
            // GIVEN
            EmpleadoResponseDTO response = empleadoResponseDTOEjemplo();
            response.setCuentaActiva(false);

            when(empleadoService.cambiarEstadoCuenta(eq(1L), eq(false))).thenReturn(response);

            String body = """
                    { "activo": false }
                    """;

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/1/cuenta")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cuentaActiva").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, activar cuenta existente → devuelve 200")
        void testActivarCuenta_rolAdmin_200() throws Exception {
            // GIVEN
            EmpleadoResponseDTO response = empleadoResponseDTOEjemplo();
            response.setCuentaActiva(true);

            when(empleadoService.cambiarEstadoCuenta(eq(1L), eq(true))).thenReturn(response);

            String body = """
                    { "activo": true }
                    """;

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/1/cuenta")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cuentaActiva").value(true));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testGestionarCuenta_rolEmpleado_403() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/1/cuenta")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"activo\": false }"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ID inexistente → devuelve 404 Not Found")
        void testGestionarCuenta_noExiste_404() throws Exception {
            // GIVEN
            when(empleadoService.cambiarEstadoCuenta(eq(999L), anyBoolean()))
                    .thenThrow(new ResourceNotFoundException("Empleado no encontrado con id: 999"));

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/999/cuenta")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"activo\": false }"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PATCH /empleados/{id}/rol
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /empleados/{id}/rol — Cambiar rol de usuario")
    class CambiarRol {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, cambio de rol válido → devuelve 200")
        void testCambiarRol_rolAdmin_200() throws Exception {
            // GIVEN
            EmpleadoResponseDTO response = empleadoResponseDTOEjemplo();
            response.setRol(RolEmpleado.ADMIN);

            when(empleadoService.cambiarRol(eq(1L), eq("ADMIN"))).thenReturn(response);

            String body = """
                    { "rol": "ADMIN" }
                    """;

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/1/rol")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testCambiarRol_rolEmpleado_403() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/1/rol")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"rol\": \"ADMIN\" }"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ID inexistente → devuelve 404 Not Found")
        void testCambiarRol_noExiste_404() throws Exception {
            // GIVEN
            when(empleadoService.cambiarRol(eq(999L), anyString()))
                    .thenThrow(new ResourceNotFoundException("Empleado no encontrado con id: 999"));

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/999/rol")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"rol\": \"EMPLEADO\" }"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE /empleados/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /empleados/{id} — Eliminar empleado")
    class EliminarEmpleado {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Rol ADMIN, empleado sin ventas → devuelve 204 No Content")
        void testEliminarEmpleado_sinVentas_204() throws Exception {
            // GIVEN
            doNothing().when(empleadoService).eliminarEmpleado(1L);

            // WHEN / THEN
            mockMvc.perform(delete(BASE_URL + "/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(empleadoService, times(1)).eliminarEmpleado(1L);
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("Rol EMPLEADO → devuelve 403 Forbidden")
        void testEliminarEmpleado_rolEmpleado_403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(empleadoService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ID inexistente → devuelve 404 Not Found")
        void testEliminarEmpleado_noExiste_404() throws Exception {
            // GIVEN
            doThrow(new ResourceNotFoundException("Empleado no encontrado con id: 999"))
                    .when(empleadoService).eliminarEmpleado(999L);

            // WHEN / THEN
            mockMvc.perform(delete(BASE_URL + "/999")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Empleado con ventas asociadas → devuelve 409 Conflict")
        void testEliminarEmpleado_conVentas_409() throws Exception {
            // GIVEN — un empleado que ya tiene ventas registradas no puede eliminarse
            doThrow(new BusinessRuleException(
                    "No se puede eliminar el empleado porque tiene ventas registradas. Considere desactivar su cuenta."))
                    .when(empleadoService).eliminarEmpleado(1L);

            // WHEN / THEN
            mockMvc.perform(delete(BASE_URL + "/1")
                            .with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensaje").exists());
        }
    }
}
