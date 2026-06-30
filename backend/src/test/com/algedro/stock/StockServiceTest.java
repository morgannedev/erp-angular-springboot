package com.algedro.stock;

import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.proveedor.repository.ProveedorRepository;
import com.algedro.stock.dto.MovimientoStockResponse;
import com.algedro.stock.dto.StockAjusteRequest;
import com.algedro.stock.dto.StockEntradaRequest;
import com.algedro.stock.dto.StockNivelResponse;
import com.algedro.stock.entity.MovimientoStock;
import com.algedro.stock.entity.TipoMovimientoStock;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("StockService - Fase 5 RED")
class StockServiceTest {

    private ProductoRepository productoRepository;
    private ProveedorRepository proveedorRepository;
    private MovimientoStockRepository movimientoStockRepository;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        proveedorRepository = mock(ProveedorRepository.class);
        movimientoStockRepository = mock(MovimientoStockRepository.class);
        stockService = new StockService(
                productoRepository,
                proveedorRepository,
                movimientoStockRepository
        );
    }

    @Nested
    @DisplayName("Ajustes manuales")
    class Ajustes {

        @Test
        @DisplayName("testAjuste_sinMotivo_400")
        void testAjuste_sinMotivo_400() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            StockAjusteRequest request = new StockAjusteRequest(-5, null, false);

            // Act & Assert
            assertThatThrownBy(() -> stockService.registrarAjuste(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El motivo del ajuste es obligatorio");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("testAjuste_motivoMenosDe10Chars_400")
        void testAjuste_motivoMenosDe10Chars_400() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            StockAjusteRequest request = new StockAjusteRequest(-5, "rotura", false);

            // Act & Assert
            assertThatThrownBy(() -> stockService.registrarAjuste(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El motivo del ajuste debe tener al menos 10 caracteres");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("testStock_nuncaNegativo_sinConfirmacion")
        void testStock_nuncaNegativo_sinConfirmacion() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            StockAjusteRequest request = new StockAjusteRequest(-100, "regularizacion inventario", false);

            // Act & Assert
            assertThatThrownBy(() -> stockService.registrarAjuste(1L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("El ajuste dejaria el stock en negativo");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("testAjuste_cantidadCero_400")
        void testAjuste_cantidadCero_400() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            StockAjusteRequest request = new StockAjusteRequest(0, "ajuste de inventario", false);

            // Act & Assert
            assertThatThrownBy(() -> stockService.registrarAjuste(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La cantidad del ajuste no puede ser cero");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Movimientos")
    class Movimientos {

        @Test
        @DisplayName("testEntrada_actualizaStockActual")
        void testEntrada_actualizaStockActual() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 48);
            Proveedor proveedor = createProveedor(1L, "Proveedor Test");

            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

            MovimientoStock movimientoGuardado = createMovimientoStock(
                    producto, TipoMovimientoStock.ENTRADA, 12, 60, null, null, proveedor, "ALB-2026-001", null
            );
            when(movimientoStockRepository.save(any(MovimientoStock.class))).thenReturn(movimientoGuardado);

            // Act
            StockEntradaRequest request = new StockEntradaRequest(12, 1L, "ALB-2026-001");
            MovimientoStockResponse response = stockService.registrarEntrada(1L, request);

            // Assert
            assertThat(response.stockResultante()).isEqualTo(60);
            assertThat(response.cantidad()).isEqualTo(12);
            assertThat(response.tipo()).isEqualTo("ENTRADA");
            assertThat(response.productoId()).isEqualTo(1L);

            // Verificar que se actualizó el stock del producto
            assertThat(producto.getStockActual()).isEqualTo(60);
            verify(productoRepository).save(producto);
            verify(movimientoStockRepository).save(any(MovimientoStock.class));
        }

        @Test
        @DisplayName("testDescuentoAutomatico_porVenta")
        void testDescuentoAutomatico_porVenta() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 48);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            MovimientoStock movimientoGuardado = createMovimientoStock(
                    producto, TipoMovimientoStock.VENTA, -3, 45, null, 10L, null, null, null
            );
            when(movimientoStockRepository.save(any(MovimientoStock.class))).thenReturn(movimientoGuardado);

            // Act
            MovimientoStockResponse response = stockService.descontarPorVenta(1L, 3, 10L);

            // Assert
            assertThat(response.tipo()).isEqualTo("VENTA");
            assertThat(response.cantidad()).isEqualTo(-3);
            assertThat(response.stockResultante()).isEqualTo(45);
            assertThat(response.productoId()).isEqualTo(1L);
            assertThat(producto.getStockActual()).isEqualTo(45);

            verify(productoRepository).save(producto);
            verify(movimientoStockRepository).save(any(MovimientoStock.class));
        }

        @Test
        @DisplayName("testEntrada_productoNoEncontrado_lanzaExcepcion")
        void testEntrada_productoNoEncontrado_lanzaExcepcion() throws Exception {
            // Arrange
            when(productoRepository.findById(999L)).thenReturn(Optional.empty());

            StockEntradaRequest request = new StockEntradaRequest(10, 1L, "ALB-001");

            // Act & Assert
            assertThatThrownBy(() -> stockService.registrarEntrada(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Producto no encontrado");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("testVenta_stockInsuficiente_lanzaExcepcion")
        void testVenta_stockInsuficiente_lanzaExcepcion() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 2);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act & Assert
            assertThatThrownBy(() -> stockService.descontarPorVenta(1L, 5, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("La venta dejaria el stock en negativo");

            verify(productoRepository, never()).save(any());
            verify(movimientoStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("testEntrada_conProveedorNull_permite")
        void testEntrada_conProveedorNull_permite() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 48);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            MovimientoStock movimientoGuardado = createMovimientoStock(
                    producto, TipoMovimientoStock.ENTRADA, 12, 60, null, null, null, "ALB-001", null
            );
            when(movimientoStockRepository.save(any(MovimientoStock.class))).thenReturn(movimientoGuardado);

            // Act
            StockEntradaRequest request = new StockEntradaRequest(12, null, "ALB-001");
            MovimientoStockResponse response = stockService.registrarEntrada(1L, request);

            // Assert
            assertThat(response.stockResultante()).isEqualTo(60);
            assertThat(response.cantidad()).isEqualTo(12);
            assertThat(response.proveedorId()).isNull();

            verify(proveedorRepository, never()).findById(any());
            verify(productoRepository).save(producto);
            verify(movimientoStockRepository).save(any(MovimientoStock.class));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("listarStock soporta filtro de alertas")
        void listarStock_soportaFiltroDeAlertas() throws Exception {
            // Arrange
            Producto producto1 = createProducto(1L, "Producto 1", 5);
            producto1.setStockMinimo(10);
            producto1.setEnAlerta(true);

            Producto producto2 = createProducto(2L, "Producto 2", 50);
            producto2.setStockMinimo(10);
            producto2.setEnAlerta(false);

            Page<Producto> page = new PageImpl<>(List.of(producto1, producto2));
            when(productoRepository.buscarActivos(anyString(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // Act
            Page<StockNivelResponse> result = stockService.listarStock(
                    true, null, null, null, PageRequest.of(0, 25)
            );

            // Assert
            assertThat(result).isNotNull();
            // Con filtro de alertas, solo debería mostrar productos en alerta
            assertThat(result.getContent())
                    .allMatch(StockNivelResponse::enAlerta);

            verify(productoRepository).buscarActivos(anyString(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("listarStock_sinFiltros_retornaTodos")
        void listarStock_sinFiltros_retornaTodos() throws Exception {
            // Arrange
            Producto producto1 = createProducto(1L, "Producto 1", 5);
            Producto producto2 = createProducto(2L, "Producto 2", 50);

            Page<Producto> page = new PageImpl<>(List.of(producto1, producto2));
            when(productoRepository.buscarActivos(anyString(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // Act
            Page<StockNivelResponse> result = stockService.listarStock(
                    false, null, null, null, PageRequest.of(0, 25)
            );

            // Assert
            assertThat(result.getContent()).hasSize(2);
            verify(productoRepository).buscarActivos(anyString(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("listarStock_filtraPorProductoId")
        void listarStock_filtraPorProductoId() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            Page<Producto> page = new PageImpl<>(List.of(producto));
            when(productoRepository.buscarActivos(anyString(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // Act
            Page<StockNivelResponse> result = stockService.listarStock(
                    false, 1L, null, null, PageRequest.of(0, 25)
            );

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).productoId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Actualización de mínimos y máximos")
    class ActualizacionLimites {

        @Test
        @DisplayName("actualizarStockMinimo_exitoso")
        void actualizarStockMinimo_exitoso() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            producto.setStockMinimo(5);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act
            stockService.actualizarStockMinimo(1L, 15);

            // Assert
            assertThat(producto.getStockMinimo()).isEqualTo(15);
            verify(productoRepository).save(producto);
        }

        @Test
        @DisplayName("actualizarStockMinimo_valorNegativo_lanzaExcepcion")
        void actualizarStockMinimo_valorNegativo_lanzaExcepcion() throws Exception {
            // Act & Assert
            assertThatThrownBy(() -> stockService.actualizarStockMinimo(1L, -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El stock mínimo no puede ser negativo");

            verify(productoRepository, never()).findById(any());
            verify(productoRepository, never()).save(any());
        }

        @Test
        @DisplayName("actualizarStockMaximo_exitoso")
        void actualizarStockMaximo_exitoso() throws Exception {
            // Arrange
            Producto producto = createProducto(1L, "Producto Test", 50);
            producto.setStockMaximo(100);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act
            stockService.actualizarStockMaximo(1L, 150);

            // Assert
            assertThat(producto.getStockMaximo()).isEqualTo(150);
            verify(productoRepository).save(producto);
        }

        @Test
        @DisplayName("actualizarStockMaximo_valorNegativo_lanzaExcepcion")
        void actualizarStockMaximo_valorNegativo_lanzaExcepcion() throws Exception {
            // Act & Assert
            assertThatThrownBy(() -> stockService.actualizarStockMaximo(1L, -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El stock máximo no puede ser negativo");

            verify(productoRepository, never()).findById(any());
            verify(productoRepository, never()).save(any());
        }
    }

    // Helper methods
    private Producto createProducto(Long id, String nombre, Integer stockActual) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStockActual(stockActual);
        producto.setStockMinimo(10);
        producto.setStockMaximo(100);
        producto.setActivo(true);
        return producto;
    }

    private Proveedor createProveedor(Long id, String nombre) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setNombre(nombre);
        return proveedor;
    }

    private MovimientoStock createMovimientoStock(
            Producto producto,
            TipoMovimientoStock tipo,
            Integer cantidad,
            Integer stockResultante,
            String motivo,
            Long ventaId,
            Proveedor proveedor,
            String albaran,
            com.algedro.empleado.entity.Empleado empleado
    ) {
        MovimientoStock movimiento = new MovimientoStock(
                producto, tipo, cantidad, stockResultante, motivo, ventaId, proveedor, albaran, empleado
        );
        // Usar reflexión para establecer el ID si es necesario
        try {
            java.lang.reflect.Field idField = MovimientoStock.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(movimiento, 1L);
        } catch (Exception e) {
            // Ignorar
        }
        return movimiento;
    }
}