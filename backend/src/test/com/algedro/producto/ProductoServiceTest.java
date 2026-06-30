package com.algedro.producto;

import com.algedro.categoria.entity.Categoria;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.service.ProductoService;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.producto.dto.ProductoRequest;
import com.algedro.producto.dto.ProductoResponse;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.exception.BusinessRuleException;
import com.algedro.categoria.repository.CategoriaRepository;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.proveedor.repository.ProveedorRepository;
import com.algedro.venta.repository.DetalleVentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitarios para ProductoService — ciclo TDD RED.
 *
 * Ejecutar con: mvn test -Dtest=ProductoServiceTest
 * Todos deben FALLAR hasta que se implemente ProductoService (paso 4.5).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService — Tests unitarios")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private ProductoService productoService;

    // ── Fixtures ──────────────────────────────────────────────

    private Categoria categoriaFija;
    private Proveedor proveedorFijo;
    private Producto productoActivo;
    private Producto productoInactivo;

    @BeforeEach
    void setUp() {
        categoriaFija = Categoria.builder()
                .id(1L)
                .nombre("Limpieza del Hogar")
                .activo(true)
                .build();

        proveedorFijo = Proveedor.builder()
                .id(1L)
                .nombre("Distribuciones Químicas del Sur S.L.")
                .activo(true)
                .build();

        productoActivo = Producto.builder()
                .id(1L)
                .referencia("LIM-001")
                .ean("8410030041002")
                .nombre("Fregasuelos Pino 1L")
                .categoria(categoriaFija)
                .proveedor(proveedorFijo)
                .precioVenta(new BigDecimal("2.95"))
                .precioCoste(new BigDecimal("1.40"))
                .stockActual(48)
                .stockMinimo(10)
                .unidadMedida("ud")
                .activo(true)
                .build();

        productoInactivo = Producto.builder()
                .id(2L)
                .referencia("LIM-INV")
                .ean("8410030099001")
                .nombre("Producto Descatalogado")
                .categoria(categoriaFija)
                .precioVenta(new BigDecimal("1.00"))
                .stockActual(0)
                .stockMinimo(0)
                .unidadMedida("ud")
                .activo(false)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // CREAR PRODUCTO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("testCrear_eanDuplicado_409 — EAN ya existe → ConflictException")
        void testCrear_eanDuplicado_409() {
            // GIVEN — ya hay un producto con ese EAN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .ean("8410030041002") // EAN ya usado por productoActivo
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .build();

            given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoriaFija));
            given(productoRepository.existsByEan("8410030041002")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.crear(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("EAN");
        }

        @Test
        @DisplayName("crear_referenciaDuplicada_409 — SKU ya existe → ConflictException")
        void testCrear_referenciaDuplicada_409() {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-001") // referencia ya existente
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .build();

            given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoriaFija));
            given(productoRepository.existsByReferencia("LIM-001")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.crear(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("referencia");
        }

        @Test
        @DisplayName("crear_categoriaNoExiste_404 → ResourceNotFoundException")
        void testCrear_categoriaNoExiste_404() {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .nombre("Producto Nuevo")
                    .categoriaId(99L) // no existe
                    .precioVenta(new BigDecimal("3.00"))
                    .build();

            given(categoriaRepository.findById(99L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.crear(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("crear_exitoso — guarda y devuelve el producto creado")
        void testCrear_exitoso() {
            // GIVEN
            ProductoRequest request = ProductoRequest.builder()
                    .referencia("LIM-999")
                    .ean("1234567890123")
                    .nombre("Producto Nuevo")
                    .categoriaId(1L)
                    .proveedorId(1L)
                    .precioVenta(new BigDecimal("3.00"))
                    .precioCoste(new BigDecimal("1.50"))
                    .build();

            Producto productoGuardado = Producto.builder()
                    .id(99L)
                    .referencia("LIM-999")
                    .ean("1234567890123")
                    .nombre("Producto Nuevo")
                    .categoria(categoriaFija)
                    .proveedor(proveedorFijo)
                    .precioVenta(new BigDecimal("3.00"))
                    .precioCoste(new BigDecimal("1.50"))
                    .stockActual(0)
                    .stockMinimo(0)
                    .unidadMedida("ud")
                    .activo(true)
                    .build();

            given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoriaFija));
            given(proveedorRepository.findById(1L)).willReturn(Optional.of(proveedorFijo));
            given(productoRepository.existsByEan("1234567890123")).willReturn(false);
            given(productoRepository.existsByReferencia("LIM-999")).willReturn(false);
            given(productoRepository.save(any(Producto.class))).willReturn(productoGuardado);

            // WHEN
            ProductoResponse response = productoService.crear(request);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(99L);
            assertThat(response.getReferencia()).isEqualTo("LIM-999");
            then(productoRepository).should().save(any(Producto.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    // ELIMINAR PRODUCTO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("testEliminar_conMovimientos_bloqueado — producto con historial → BusinessRuleException")
        void testEliminar_conMovimientos_bloqueado() {
            // GIVEN — el producto tiene movimientos de stock registrados
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));
            given(movimientoStockRepository.existsByProductoId(1L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.eliminar(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("movimiento");

            then(productoRepository).should(never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("testEliminar_conLineasVenta_bloqueado — producto con ventas → BusinessRuleException")
        void testEliminar_conLineasVenta_bloqueado() {
            // GIVEN — el producto tiene líneas de venta (aunque no haya movimientos)
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));
            given(movimientoStockRepository.existsByProductoId(1L)).willReturn(false);
            given(detalleVentaRepository.existsByProductoId(1L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.eliminar(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("venta");

            then(productoRepository).should(never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("testEliminar_sinMovimientos_exitoso — producto sin historial → se elimina")
        void testEliminar_sinMovimientos_exitoso() {
            // GIVEN — producto limpio, sin movimientos ni ventas
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));
            given(movimientoStockRepository.existsByProductoId(1L)).willReturn(false);
            given(detalleVentaRepository.existsByProductoId(1L)).willReturn(false);
            willDoNothing().given(productoRepository).deleteById(1L);

            // WHEN
            productoService.eliminar(1L);

            // THEN
            then(productoRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("eliminar_noExiste_404 → ResourceNotFoundException")
        void testEliminar_noExiste_404() {
            // GIVEN
            given(productoRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.eliminar(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    // DESACTIVAR PRODUCTO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstado {

        @Test
        @DisplayName("testDesactivar_noEliminaRegistro — desactivar marca activo=false pero no borra")
        void testDesactivar_noEliminaRegistro() {
            // GIVEN
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));
            given(productoRepository.save(any(Producto.class))).willAnswer(inv -> inv.getArgument(0));

            // WHEN
            ProductoResponse response = productoService.cambiarEstado(1L, false);

            // THEN — registro sigue en BD (no se llama a delete)
            assertThat(response.isActivo()).isFalse();
            then(productoRepository).should().save(any(Producto.class));
            then(productoRepository).should(never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("reactivar_exitoso — activo=true devuelve producto activo")
        void testReactivar_exitoso() {
            // GIVEN
            given(productoRepository.findById(2L)).willReturn(Optional.of(productoInactivo));
            given(productoRepository.save(any(Producto.class))).willAnswer(inv -> inv.getArgument(0));

            // WHEN
            ProductoResponse response = productoService.cambiarEstado(2L, true);

            // THEN
            assertThat(response.isActivo()).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════
    // BÚSQUEDA POR EAN
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("buscarPorEan()")
    class BuscarPorEan {

        @Test
        @DisplayName("testBuscarPorEan_productoInactivo_noAparece — EAN de producto inactivo → vacío/404")
        void testBuscarPorEan_productoInactivo_noAparece() {
            // GIVEN — hay un producto con ese EAN pero está inactivo
            given(productoRepository.findByEanAndActivoTrue("8410030099001"))
                    .willReturn(Optional.empty()); // activo=false → no se devuelve

            // WHEN / THEN
            assertThatThrownBy(() -> productoService.buscarPorEan("8410030099001"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("buscarPorEan_activo_exitoso — EAN de producto activo → devuelve producto")
        void testBuscarPorEan_activo_exitoso() {
            // GIVEN
            given(productoRepository.findByEanAndActivoTrue("8410030041002"))
                    .willReturn(Optional.of(productoActivo));

            // WHEN
            ProductoResponse response = productoService.buscarPorEan("8410030041002");

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getEan()).isEqualTo("8410030041002");
        }
    }

    // ══════════════════════════════════════════════════════════
    // PRECIO COSTE SEGÚN ROL
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("precioCoste según rol")
    class PrecioCosteRol {

        @Test
        @DisplayName("testGetPrecioCoste_rolEmpleado_nulo — EMPLEADO recibe precioCoste=null")
        void testGetPrecioCoste_rolEmpleado_nulo() {
            // GIVEN
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));

            // WHEN — llamada con isAdmin=false (rol EMPLEADO)
            ProductoResponse response = productoService.getById(1L, false);

            // THEN — el campo precioCoste debe ser null en la respuesta
            assertThat(response.getPrecioCoste()).isNull();
        }

        @Test
        @DisplayName("getById_rolAdmin_incluyePrecioCoste — ADMIN recibe precioCoste con valor")
        void testGetPrecioCoste_rolAdmin_visible() {
            // GIVEN
            given(productoRepository.findById(1L)).willReturn(Optional.of(productoActivo));

            // WHEN — llamada con isAdmin=true (rol ADMIN)
            ProductoResponse response = productoService.getById(1L, true);

            // THEN — precioCoste visible
            assertThat(response.getPrecioCoste()).isEqualByComparingTo(new BigDecimal("1.40"));
        }

        @Test
        @DisplayName("listar_rolEmpleado_precioCosteNulo — en paginación EMPLEADO todos tienen precioCoste=null")
        void testListar_rolEmpleado_precioCosteNulo() {
            // GIVEN
            Page<Producto> pagina = new PageImpl<>(List.of(productoActivo));
            // ✅ CORREGIDO - Usar isNull() para parámetros que pueden ser null
            given(productoRepository.buscarActivos(
                    isNull(),        // categoriaId - puede ser null
                    isNull(),        // proveedorId - puede ser null
                    anyString(),     // query
                    eq(true),        // activo - para empleado siempre true
                    any(Pageable.class)
            )).willReturn(pagina);

            // WHEN
            var resultado = productoService.listar(null, null, null, true, PageRequest.of(0, 25), false);

            // THEN
            assertThat(resultado.getContent())
                    .allSatisfy(p -> assertThat(p.getPrecioCoste()).isNull());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PAGINACIÓN Y LISTADO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("listar_sinFiltros_devuelveActivos — por defecto solo activos")
        void testListar_sinFiltros_devuelveActivos() {
            // GIVEN — 25 productos activos en BD
            List<Producto> productos = java.util.Collections.nCopies(25, productoActivo);
            Page<Producto> pagina = new PageImpl<>(productos, PageRequest.of(0, 25), 25);

            // ✅ CORREGIDO - Usar isNull() para parámetros que pueden ser null
            given(productoRepository.buscarActivos(
                    isNull(),        // categoriaId - puede ser null
                    isNull(),        // proveedorId - puede ser null
                    anyString(),     // query - String (no puede ser null en el método real)
                    isNull(),        // activo - puede ser null (admin ve todos)
                    any(Pageable.class)
            )).willReturn(pagina);

            // WHEN
            var resultado = productoService.listar(null, null, null, true, PageRequest.of(0, 25), true);

            // THEN
            assertThat(resultado.getTotalElements()).isEqualTo(25);

            // Verificar que se llamó con los argumentos correctos
            then(productoRepository).should().buscarActivos(
                    isNull(),        // categoriaId
                    isNull(),        // proveedorId
                    eq(""),          // query
                    isNull(),        // activo (admin ve todos)
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("listar_conFiltros_devuelveResultadosFiltrados")
        void testListar_conFiltros_devuelveResultadosFiltrados() {
            // GIVEN
            List<Producto> productos = List.of(productoActivo);
            Page<Producto> pagina = new PageImpl<>(productos, PageRequest.of(0, 25), 1);

            given(productoRepository.buscarActivos(
                    eq(1L),          // categoriaId
                    eq(1L),          // proveedorId
                    eq("pino"),      // query
                    eq(true),        // activo
                    any(Pageable.class)
            )).willReturn(pagina);

            // WHEN
            var resultado = productoService.listar(1L, 1L, "pino", true, PageRequest.of(0, 25), false);

            // THEN
            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Fregasuelos Pino 1L");
        }
    }
}