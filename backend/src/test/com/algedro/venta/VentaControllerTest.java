package com.algedro.venta;

import com.algedro.exception.ForbiddenException;
import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.security.TokenBlacklistService;
import com.algedro.venta.controller.VentaController;
import com.algedro.venta.service.VentaService;
import com.algedro.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VentaController.class)
@DisplayName("VentaController — Tests de Capa Web")
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VentaService ventaService;

    @MockBean
    private EmpleadoRepository empleadoRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;


    @Nested
    @DisplayName("POST /ventas/{ventaId}/anular")
    class AnularVentaWeb {

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("testAnular_rolEmpleado_403 → Un empleado no tiene autorización")
        void testAnular_rolEmpleado_403() throws Exception {
            Map<String, String> body = Map.of("motivoAnulacion", "Error de cajero");

            // El endpoint /anular está protegido con @PreAuthorize("hasRole('ADMIN')")
            // Spring Security rechaza la petición antes de llegar al service
            mockMvc.perform(post("/ventas/1/anular")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /ventas")
    class ListarVentasWeb {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("testHistorial_filtrosFecha_200 → ADMIN filtra correctamente por fechas")
        void testHistorial_filtrosFecha_200() throws Exception {
            // Los parámetros opcionales (empleadoId, clienteId, metodoPago, estado) pueden ser
            // null; se usa any() para no acoplar el test a la firma exacta del service
            given(ventaService.listar(
                    any(LocalDate.class),
                    any(LocalDate.class),
                    any(),   // empleadoId — nullable Long
                    any(),   // clienteId  — nullable Long
                    any(),   // metodoPago — nullable String
                    any(),   // estado     — nullable String
                    any(PageRequest.class),
                    anyString()  // rol del usuario autenticado
            )).willReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(get("/ventas")
                            .param("desde", "2026-01-01")
                            .param("hasta", "2026-05-28")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(roles = "EMPLEADO")
        @DisplayName("testExportarCsv_rolEmpleado_403 → Un empleado no puede exportar el historial en CSV")
        void testExportarCsv_rolEmpleado_403() throws Exception {
            // GET /ventas es accesible para EMPLEADO (x-roles: [ADMIN, EMPLEADO]),
            // pero exportar=true es una operación restringida a ADMIN.
            // El controller delega la comprobación al service, que lanza ForbiddenException
            // cuando el rol es EMPLEADO y exportar=true; el GlobalExceptionHandler mapea a 403.
            given(ventaService.listar(any(), any(), any(), any(), any(), any(), any(), anyString()))
                    .willThrow(new ForbiddenException("Exportación CSV restringida a ADMIN"));

            mockMvc.perform(get("/ventas")
                            .param("exportar", "true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}
