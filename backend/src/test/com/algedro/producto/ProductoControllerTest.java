package com.algedro.producto;

import com.algedro.producto.controller.ProductoController;
import com.algedro.producto.dto.ProductoRequest;
import com.algedro.producto.dto.ProductoResponse;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.producto.service.ProductoService;
import com.algedro.security.JwtUtils;
import com.algedro.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de slice para ProductoController — ciclo TDD RED.
 *
 * Ejecutar con: mvn test -Dtest=ProductoControllerTest
 * Todos deben FALLAR hasta que se implemente ProductoController (paso 4.7).
 */
@WebMvcTest(ProductoController.class)
@DisplayName("ProductoController — Tests de slice WebMvcTest")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    private static final String BASE_URL = "/productos";

    // ── Fixtures helpers ──────────────────────────────────────

    private ProductoResponse responseConPrecioCoste() {
        return ProductoResponse.builder()
                .id(1L)
                .referencia("LIM-001")
                .ean("8410030041002")
                .nombre("Fregasuelos Pino 1L")
                .precioVenta(new BigDecimal("2.95"))
                .precioCoste(new BigDecimal("1.40"))
                .stockActual(48)
                .stockMinimo(10)
                .unidadMedida("ud")
                .activo(true)
                .build();
    }

    private ProductoResponse responseSinPrecioCoste() {
        ProductoResponse r = responseConPrecioCoste();
        r.setPrecioCoste(null); // EMPLEADO no recibe precio coste
        return r;
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos/barcode/{ean}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /productos/barcode/{ean}")
    class BarcodeEndpoint {

        @Test
        @DisplayName("testBusquedaBarcode_200 — EAN válido activo → 200 con datos del producto")
        @WithMockUser(roles = "EMPLEADO")
        void testBusquedaBarcode_200() throws Exception {
            // GIVEN
            given(productoService.buscarPorEan("8410030041002"))
                    .willReturn(responseSinPrecioCoste());

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/barcode/8410030041002"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.ean").value("8410030041002"))
                    .andExpect(jsonPath("$.nombre").value("Fregasuelos Pino 1L"))
                    .andExpect(jsonPath("$.precioCoste").doesNotExist()); // EMPLEADO no ve precioCoste
        }

        @Test
        @DisplayName("busquedaBarcode_eanNoExiste_404 — EAN desconocido → 404")
        @WithMockUser(roles = "EMPLEADO")
        void testBusquedaBarcode_404() throws Exception {
            // GIVEN
            given(productoService.buscarPorEan("0000000000000"))
                    .willThrow(new ResourceNotFoundException("Producto no encontrado con EAN: 0000000000000"));

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/barcode/0000000000000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("busquedaBarcode_sinAutenticacion_401 — sin JWT → 401")
        void testBusquedaBarcode_sinJwt_401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/barcode/8410030041002"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("busquedaBarcode_rolAdmin_incluyePrecioCoste — ADMIN ve precioCoste en barcode")
        @WithMockUser(roles = "ADMIN")
        void testBusquedaBarcode_rolAdmin_incluyePrecioCoste() throws Exception {
            // GIVEN
            given(productoService.buscarPorEan("8410030041002"))
                    .willReturn(responseConPrecioCoste());

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/barcode/8410030041002"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.precioCoste").value(1.40));
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /productos
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /productos")
    class CrearProducto {

        @Test
        @DisplayName("testCrear_rolEmpleado_403 — EMPLEADO no puede crear productos → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testCrear_rolEmpleado_403() throws Exception {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .build();

            // WHEN / THEN — el rol EMPLEADO debe recibir 403 Forbidden
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            // El service no debe ser invocado
            then(productoService).should(never()).crear(any());
        }

        @Test
        @DisplayName("crear_rolAdmin_201 — ADMIN crea producto válido → 201")
        @WithMockUser(roles = "ADMIN")
        void testCrear_rolAdmin_201() throws Exception {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .ean("1234567890123")
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .precioCoste(new BigDecimal("1.50"))
                    .build();

            ProductoResponse responseCreado = responseConPrecioCoste();
            responseCreado.setId(99L);
            responseCreado.setReferencia("LIM-999");

            given(productoService.crear(any(ProductoRequest.class))).willReturn(responseCreado);

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(99));
        }

        @Test
        @DisplayName("crear_eanDuplicado_409 — EAN duplicado → 409 Conflict")
        @WithMockUser(roles = "ADMIN")
        void testCrear_eanDuplicado_409() throws Exception {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .ean("8410030041002") // EAN existente
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .build();

            given(productoService.crear(any())).willThrow(new ConflictException("EAN duplicado: 8410030041002"));

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").exists());
        }

        @Test
        @DisplayName("crear_camposObligatoriosAusentes_400 — request inválido → 400 Bad Request")
        @WithMockUser(roles = "ADMIN")
        void testCrear_camposObligatoriosAusentes_400() throws Exception {
            // GIVEN — faltan referencia, nombre y precioVenta
            String requestInvalido = "{}";

            // WHEN / THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestInvalido))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos (paginado)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /productos")
    class ListarProductos {

        @Test
        @DisplayName("testGetPaginado_parametrosPorDefecto_25elementos — sin params → page=0, size=25")
        @WithMockUser(roles = "ADMIN")
        void testGetPaginado_parametrosPorDefecto_25elementos() throws Exception {
            // GIVEN — se espera que use page=0 y size=25 por defecto
            List<ProductoResponse> items = Collections.nCopies(25, responseConPrecioCoste());
            var pagina = new PageImpl<>(items, PageRequest.of(0, 25), 25);

            given(productoService.listar(
                    isNull(),   // q (búsqueda)
                    isNull(),   // categoriaId
                    isNull(),   // proveedorId
                    eq(true),   // activo (default)
                    eq(PageRequest.of(0, 25)),
                    eq(true)    // isAdmin
            )).willReturn(pagina);

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido").isArray())
                    .andExpect(jsonPath("$.contenido.length()").value(25))
                    .andExpect(jsonPath("$.totalElementos").value(25))
                    .andExpect(jsonPath("$.pagina").value(0))
                    .andExpect(jsonPath("$.tamano").value(25));
        }

        @Test
        @DisplayName("getPaginado_conBusqueda_filtraResultados — ?q=pino → filtra por nombre")
        @WithMockUser(roles = "EMPLEADO")
        void testGetPaginado_conBusqueda() throws Exception {
            // GIVEN
            var pagina = new PageImpl<>(List.of(responseSinPrecioCoste()), PageRequest.of(0, 25), 1);

            given(productoService.listar(
                    isNull(),           // 1. Long categoriaId
                    isNull(),           // 2. Long proveedorId
                    eq("pino"),         // 3. String query (q)
                    eq(true),           // 4. Boolean activo
                    any(),              // 5. Pageable pageable
                    eq(false)           // 6. boolean isAdmin
            )).willReturn(pagina);

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL).param("q", "pino"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido.length()").value(1))
                    .andExpect(jsonPath("$.contenido[0].precioCoste").doesNotExist());
        }

        @Test
        @DisplayName("getPaginado_sinAutenticacion_401 — sin JWT → 401")
        void testGetPaginado_sinJwt_401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /productos/{id}")
    class GetProductoPorId {

        @Test
        @DisplayName("getById_rolAdmin_200_conPrecioCoste — ADMIN ve todos los campos")
        @WithMockUser(roles = "ADMIN")
        void testGetById_rolAdmin_200() throws Exception {
            // GIVEN
            given(productoService.getById(1L, true)).willReturn(responseConPrecioCoste());

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.precioCoste").value(1.40));
        }

        @Test
        @DisplayName("getById_rolEmpleado_200_sinPrecioCoste — EMPLEADO no ve precioCoste")
        @WithMockUser(roles = "EMPLEADO")
        void testGetById_rolEmpleado_200_sinPrecioCoste() throws Exception {
            // GIVEN
            given(productoService.getById(1L, false)).willReturn(responseSinPrecioCoste());

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.precioCoste").doesNotExist());
        }

        @Test
        @DisplayName("getById_noExiste_404 — ID inexistente → 404")
        @WithMockUser(roles = "ADMIN")
        void testGetById_noExiste_404() throws Exception {
            // GIVEN
            given(productoService.getById(999L, true))
                    .willThrow(new ResourceNotFoundException("Producto no encontrado: 999"));

            // WHEN / THEN
            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /productos/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /productos/{id}")
    class EliminarProducto {

        @Test
        @DisplayName("eliminar_rolAdmin_sinHistorial_204 — ADMIN elimina producto limpio → 204")
        @WithMockUser(roles = "ADMIN")
        void testEliminar_rolAdmin_204() throws Exception {
            // GIVEN
            willDoNothing().given(productoService).eliminar(1L);

            // WHEN / THEN
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("eliminar_rolEmpleado_403 — EMPLEADO no puede eliminar → 403")
        @WithMockUser(roles = "EMPLEADO")
        void testEliminar_rolEmpleado_403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("eliminar_conMovimientos_409 — producto con historial → 409 Conflict")
        @WithMockUser(roles = "ADMIN")
        void testEliminar_conMovimientos_409() throws Exception {
            // GIVEN — Corregido para métodos 'void': Primero la excepción, luego el objetivo del mock
            willThrow(new com.algedro.exception.BusinessRuleException("El producto tiene movimientos registrados"))
                    .given(productoService).eliminar(1L);
            // WHEN / THEN
            mockMvc.perform(delete(BASE_URL + "/1").with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /productos/{id}/estado
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /productos/{id}/estado")
    class CambiarEstado {

        @Test
        @DisplayName("cambiarEstado_desactivar_rolAdmin_200 — ADMIN desactiva → 200")
        @WithMockUser(roles = "ADMIN")
        void testCambiarEstado_desactivar_200() throws Exception {
            // GIVEN
            ProductoResponse desactivado = responseConPrecioCoste();
            desactivado.setActivo(false);
            given(productoService.cambiarEstado(1L, false)).willReturn(desactivado);

            // WHEN / THEN
            mockMvc.perform(patch(BASE_URL + "/1/estado")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(false));
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
        }
    }
}
