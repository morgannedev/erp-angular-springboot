package com.algedro.cliente;

import com.algedro.cliente.controller.ClienteController;
import com.algedro.cliente.dto.ClienteRequest;
import com.algedro.cliente.dto.ClienteResponse;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.cliente.service.ClienteService;
import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de slice para ClienteController — ciclo TDD RED (Fase 6.6).
 *
 * Ejecutar con: mvn test -Dtest=ClienteControllerTest
 *
 * Todos los tests deben FALLAR hasta que se implemente ClienteController
 * y se registre la SecurityConfig con @EnableMethodSecurity.
 *
 * Reglas de seguridad verificadas:
 *   - Sin autenticación               → 401
 *   - ROLE_EMPLEADO, sólo lectura     → 403 en escritura/borrado/cambio de estado
 *   - ROLE_ADMIN, acceso completo     → 2xx en todas las operaciones
 *
 * Campos sensibles:
 *   - nif: ADMIN lo ve siempre; EMPLEADO lo ve enmascarado (ej. "***45678")
 *     o directamente null según decisión de diseño.
 *   - email: visible para ambos roles.
 */
@WebMvcTest(ClienteController.class)
@DisplayName("ClienteController — Tests de slice WebMvcTest")
@EnableMethodSecurity
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    private static final String BASE_URL = "/clientes";

    // ── Fixtures helpers ──────────────────────────────────────

    private ClienteResponse responseCompleto() {
        return ClienteResponse.builder()
                .id(1L)
                .nombre("Ana")
                .apellidos("García López")
                .email("ana.garcia@example.com")
                .telefono("612345678")
                .nif("12345678A")
                .direccion("Calle Mayor, 10, Madrid")
                .activo(true)
                .build();
    }

    /**
     * EMPLEADO no recibe el NIF completo — se devuelve null o enmascarado.
     * Los tests verifican que el campo no tenga el valor real "12345678A".
     */
    private ClienteResponse responseEmpleado() {
        ClienteResponse r = responseCompleto();
        r.setNif(null); // El service/controller suprime el NIF para roles no-admin
        return r;
    }

    // ══════════════════════════════════════════════════════════
    // POST /clientes
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /clientes")
    class CrearCliente {

        @Test
        @DisplayName("crear_rolAdmin_201 — ADMIN crea cliente válido → 201 con Location header")
        @WithMockUser(roles = "ADMIN")
        void testCrear_rolAdmin_201() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García López")
                    .email("ana.garcia@example.com")
                    .telefono("612345678")
                    .nif("12345678A")
                    .build();

            ClienteResponse creado = responseCompleto();
            creado.setId(10L);

            given(clienteService.crear(any(ClienteRequest.class))).willReturn(creado);

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.email").value("ana.garcia@example.com"));
        }

        @Test
        @DisplayName("crear_rolEmpleado_403 — EMPLEADO no puede crear clientes → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testCrear_rolEmpleado_403() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García López")
                    .email("ana@example.com")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(clienteService).should(never()).crear(any());
        }

        @Test
        @DisplayName("crear_sinAutenticacion_401 — sin JWT → 401")
        void testCrear_sinJwt_401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("crear_emailDuplicado_409 — email ya registrado → 409 Conflict con mensaje")
        @WithMockUser(roles = "ADMIN")
        void testCrear_emailDuplicado_409() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García López")
                    .email("ana.garcia@example.com")
                    .build();

            given(clienteService.crear(any()))
                    .willThrow(new ConflictException("El email 'ana.garcia@example.com' ya está registrado"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").exists());
        }

        @Test
        @DisplayName("crear_nifDuplicado_409 — NIF ya registrado → 409 Conflict")
        @WithMockUser(roles = "ADMIN")
        void testCrear_nifDuplicado_409() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Pedro")
                    .apellidos("Sánchez")
                    .email("pedro@example.com")
                    .nif("12345678A") // NIF ya en uso
                    .build();

            given(clienteService.crear(any()))
                    .willThrow(new ConflictException("El NIF '12345678A' ya está registrado"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").exists());
        }

        @Test
        @DisplayName("crear_camposObligatoriosAusentes_400 — body vacío → 400 Bad Request")
        @WithMockUser(roles = "ADMIN")
        void testCrear_camposObligatoriosAusentes_400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /clientes  (paginado + filtros)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /clientes")
    class ListarClientes {

        @Test
        @DisplayName("listar_rolAdmin_200_conNif — ADMIN recibe NIF sin enmascarar")
        @WithMockUser(roles = "ADMIN")
        void testListar_rolAdmin_200_conNif() throws Exception {
            var items = Collections.nCopies(3, responseCompleto());
            var pagina = new PageImpl<>(items, PageRequest.of(0, 25), 3);

            // ✂️ CORREGIDO: Se elimina el cuarto parámetro eq(true)
            given(clienteService.listar(
                    isNull(),        // q
                    eq(true),        // activo default
                    eq(PageRequest.of(0, 25)) // pageable
            )).willReturn(pagina);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido").isArray())
                    .andExpect(jsonPath("$.contenido.length()").value(3))
                    .andExpect(jsonPath("$.totalElementos").value(3))
                    .andExpect(jsonPath("$.pagina").value(0))
                    .andExpect(jsonPath("$.tamano").value(25))
                    .andExpect(jsonPath("$.contenido[0].nif").value("12345678A"));
        }

        @Test
        @DisplayName("listar_rolEmpleado_200_sinNif — EMPLEADO no recibe NIF real")
        @WithMockUser(roles = "EMPLEADO")
        void testListar_rolEmpleado_200_sinNif() throws Exception {
            var pagina = new PageImpl<>(
                    List.of(responseEmpleado()), PageRequest.of(0, 25), 1);

            // ✂️ CORREGIDO: Se elimina el cuarto parámetro eq(false)
            given(clienteService.listar(
                    isNull(),
                    eq(true),
                    any(Pageable.class)
            )).willReturn(pagina);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido[0].nif").doesNotExist());
        }

        @Test
        @DisplayName("listar_conBusqueda_filtraResultados — ?q=ana → filtra por nombre/email")
        @WithMockUser(roles = "EMPLEADO")
        void testListar_conBusqueda() throws Exception {
            var pagina = new PageImpl<>(
                    List.of(responseEmpleado()), PageRequest.of(0, 25), 1);

            // ✂️ CORREGIDO: Se elimina el cuarto parámetro eq(false)
            given(clienteService.listar(
                    eq("ana"),
                    eq(true),
                    any(Pageable.class)
            )).willReturn(pagina);

            mockMvc.perform(get(BASE_URL).param("q", "ana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido.length()").value(1));
        }

        @Test
        @DisplayName("listar_inactivos_soloAdmin — ?activo=false es accesible para ADMIN")
        @WithMockUser(roles = "ADMIN")
        void testListar_inactivos_rolAdmin_200() throws Exception {
            // ✨ CORRECCIÓN: Forzamos el tipo genérico <ClienteResponse> en el List.of()
            Page<ClienteResponse> pagina = new PageImpl<>(List.<ClienteResponse>of(), PageRequest.of(0, 25), 0);

            given(clienteService.listar(isNull(), eq(false), any(Pageable.class)))
                    .willReturn(pagina);

            mockMvc.perform(get(BASE_URL).param("activo", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElementos").value(0));
        }

        @Test
        @DisplayName("listar_sinAutenticacion_401 — sin JWT → 401")
        void testListar_sinJwt_401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /clientes/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /clientes/{id}")
    class GetClientePorId {

        @Test
        @DisplayName("getById_rolAdmin_200_conNif — ADMIN ve NIF completo")
        @WithMockUser(roles = "ADMIN")
        void testGetById_rolAdmin_200() throws Exception {
            // ✂️ CORREGIDO: clienteService.getById recibe solo el ID
            given(clienteService.getById(1L)).willReturn(responseCompleto());

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nif").value("12345678A"))
                    .andExpect(jsonPath("$.email").value("ana.garcia@example.com"));
        }

        @Test
        @DisplayName("getById_rolEmpleado_200_sinNif — EMPLEADO no ve NIF real")
        @WithMockUser(roles = "EMPLEADO")
        void testGetById_rolEmpleado_200_sinNif() throws Exception {
            // ✂️ CORREGIDO: clienteService.getById recibe solo el ID
            given(clienteService.getById(1L)).willReturn(responseEmpleado());

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nif").doesNotExist());
        }

        @Test
        @DisplayName("getById_noExiste_404 — ID inexistente → 404")
        @WithMockUser(roles = "ADMIN")
        void testGetById_noExiste_404() throws Exception {
            // ✂️ CORREGIDO: clienteService.getById recibe solo el ID
            given(clienteService.getById(999L))
                    .willThrow(new ResourceNotFoundException("Cliente no encontrado: 999"));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("getById_sinAutenticacion_401 — sin JWT → 401")
        void testGetById_sinJwt_401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PUT /clientes/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /clientes/{id}")
    class ActualizarCliente {

        @Test
        @DisplayName("actualizar_rolAdmin_200 — ADMIN actualiza cliente válido → 200")
        @WithMockUser(roles = "ADMIN")
        void testActualizar_rolAdmin_200() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García López Actualizada")
                    .email("ana.nueva@example.com")
                    .telefono("699000111")
                    .build();

            ClienteResponse actualizado = responseCompleto();
            actualizado.setApellidos("García López Actualizada");
            actualizado.setEmail("ana.nueva@example.com");

            given(clienteService.actualizar(eq(1L), any(ClienteRequest.class)))
                    .willReturn(actualizado);

            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apellidos").value("García López Actualizada"))
                    .andExpect(jsonPath("$.email").value("ana.nueva@example.com"));
        }

        @Test
        @DisplayName("actualizar_rolEmpleado_403 — EMPLEADO no puede actualizar → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testActualizar_rolEmpleado_403() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García López")
                    .email("ana@example.com")
                    .build();

            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(clienteService).should(never()).actualizar(anyLong(), any());
        }

        @Test
        @DisplayName("actualizar_noExiste_404 — ID inexistente → 404")
        @WithMockUser(roles = "ADMIN")
        void testActualizar_noExiste_404() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Fantasma")
                    .apellidos("Inexistente")
                    .email("ghost@example.com")
                    .build();

            given(clienteService.actualizar(eq(999L), any()))
                    .willThrow(new ResourceNotFoundException("Cliente no encontrado: 999"));

            mockMvc.perform(put(BASE_URL + "/999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("actualizar_emailDuplicado_409 — email en uso por otro cliente → 409")
        @WithMockUser(roles = "ADMIN")
        void testActualizar_emailDuplicado_409() throws Exception {
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .apellidos("García")
                    .email("otro.cliente@example.com") // email de otro cliente
                    .build();

            given(clienteService.actualizar(eq(1L), any()))
                    .willThrow(new ConflictException("El email ya pertenece a otro cliente"));

            mockMvc.perform(put(BASE_URL + "/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /clientes/{id}/estado
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /clientes/{id}/estado")
    class CambiarEstado {

        @Test
        @DisplayName("cambiarEstado_desactivar_rolAdmin_200 — ADMIN desactiva → 200 con activo=false")
        @WithMockUser(roles = "ADMIN")
        void testCambiarEstado_desactivar_200() throws Exception {
            ClienteResponse desactivado = responseCompleto();
            desactivado.setActivo(false);

            given(clienteService.cambiarEstado(1L, false)).willReturn(desactivado);

            mockMvc.perform(patch(BASE_URL + "/1/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(false));
        }

        @Test
        @DisplayName("cambiarEstado_activar_rolAdmin_200 — ADMIN reactiva cliente → 200 con activo=true")
        @WithMockUser(roles = "ADMIN")
        void testCambiarEstado_activar_200() throws Exception {
            given(clienteService.cambiarEstado(1L, true)).willReturn(responseCompleto());

            mockMvc.perform(patch(BASE_URL + "/1/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(true));
        }

        @Test
        @DisplayName("cambiarEstado_rolEmpleado_403 — EMPLEADO no puede cambiar estado → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testCambiarEstado_rolEmpleado_403() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/1/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isForbidden());

            then(clienteService).should(never()).cambiarEstado(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("cambiarEstado_bodyVacio_400 — body sin campo 'activo' → 400")
        @WithMockUser(roles = "ADMIN")
        void testCambiarEstado_bodyVacio_400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/1/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("cambiarEstado_noExiste_404 — ID inexistente → 404")
        @WithMockUser(roles = "ADMIN")
        void testCambiarEstado_noExiste_404() throws Exception {
            given(clienteService.cambiarEstado(999L, false))
                    .willThrow(new ResourceNotFoundException("Cliente no encontrado: 999"));

            mockMvc.perform(patch(BASE_URL + "/999/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /clientes/{id}  — soft-delete (activo = false)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /clientes/{id}")
    class EliminarCliente {

        @Test
        @DisplayName("eliminar_rolAdmin_204 — ADMIN elimina (soft-delete) cliente sin historial → 204")
        @WithMockUser(roles = "ADMIN")
        void testEliminar_rolAdmin_204() throws Exception {
            willDoNothing().given(clienteService).eliminar(1L);

            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("eliminar_rolEmpleado_403 — EMPLEADO no puede eliminar → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testEliminar_rolEmpleado_403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isForbidden());

            then(clienteService).should(never()).eliminar(anyLong());
        }

        @Test
        @DisplayName("eliminar_noExiste_404 — ID inexistente → 404")
        @WithMockUser(roles = "ADMIN")
        void testEliminar_noExiste_404() throws Exception {
            willThrow(new ResourceNotFoundException("Cliente no encontrado: 999"))
                    .given(clienteService).eliminar(999L);

            mockMvc.perform(delete(BASE_URL + "/999").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("eliminar_sinAutenticacion_401 — sin JWT → 401")
        void testEliminar_sinJwt_401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
