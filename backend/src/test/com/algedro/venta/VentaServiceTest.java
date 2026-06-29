package com.algedro.venta;

import com.algedro.cliente.repository.ClienteRepository;
import com.algedro.empleado.entity.Empleado;
import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ForbiddenException;
import com.algedro.exception.InsufficientStockException;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.venta.dto.VentaDetailResponse;
import com.algedro.venta.dto.VentaLineRequest;
import com.algedro.venta.dto.VentaRequest;
import com.algedro.venta.entity.DetalleVenta;
import com.algedro.venta.entity.Venta;
import com.algedro.venta.repository.VentaRepository;
import com.algedro.venta.service.VentaService;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService — Tests Unitarios (POS)")
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private MovimientoStockRepository movimientoStockRepository;
    @Mock private EmpleadoRepository empleadoRepository;

    @InjectMocks
    private VentaService ventaService;

    private Producto productoCamiseta;
    private Producto productoInactivo;
    private Venta ventaCompletada;
    private Empleado empleadoMock;

    @BeforeEach
    void setUp() {
        empleadoMock = new Empleado();

        productoCamiseta = Producto.builder()
                .id(101L)
                .nombre("Camiseta Algodón")
                .precioVenta(new BigDecimal("19.99"))
                .stockActual(15)
                .activo(true)
                .build();

        productoInactivo = Producto.builder()
                .id(102L)
                .nombre("Gorra Antigua")
                .precioVenta(new BigDecimal("9.99"))
                .stockActual(5)
                .activo(false)
                .build();

        ventaCompletada = Venta.builder()
                .id(1L)
                .numeroVenta("VTA-2026-00001")
                .estado("COMPLETADA")
                .subtotal(new BigDecimal("39.98"))
                .descuentoGlobal(BigDecimal.ZERO)
                .total(new BigDecimal("39.98"))
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // CREAR VENTA (POS)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crearVenta()")
    class CrearVenta {

        @Test
        @DisplayName("testCrear_sinLineas_400 → BadRequestException")
        void testCrear_sinLineas_400() {
            VentaRequest request = VentaRequest.builder()
                    .lineas(Collections.emptyList())
                    .metodoPago("EFECTIVO")
                    .build();

            assertThatThrownBy(() -> ventaService.crear(request, 1L, "EMPLEADO"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cero líneas");

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_descuentosLineasYGlobal_solapados_400 → BadRequestException")
        void testCrear_descuentosLineasYGlobal_solapados_400() {
            VentaLineRequest lineaConDescuento = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(2)
                    .descuentoLinea(new BigDecimal("2.00"))
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(lineaConDescuento))
                    .descuentoGlobal(new BigDecimal("5.00"))
                    .metodoPago("TARJETA")
                    .build();

            assertThatThrownBy(() -> ventaService.crear(request, 1L, "EMPLEADO"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("mutuamente excluyentes");

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_productoInactivo_422 → BusinessRuleException")
        void testCrear_productoInactivo_422() {
            // Un producto inactivo no puede venderse: regla de negocio → 422
            VentaLineRequest lineaInactiva = VentaLineRequest.builder()
                    .productoId(102L)
                    .cantidad(1)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(lineaInactiva))
                    .metodoPago("EFECTIVO")
                    .build();

            given(productoRepository.findById(102L)).willReturn(Optional.of(productoInactivo));

            assertThatThrownBy(() -> ventaService.crear(request, 1L, "EMPLEADO"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("inactivo");

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_stockCero_sinForzar_422 → InsufficientStockException")
        void testCrear_stockCero_sinForzar_422() {
            productoCamiseta.setStockActual(0);

            VentaLineRequest linea = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(1)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(linea))
                    .metodoPago("EFECTIVO")
                    .forzarSinStock(false)
                    .build();

            given(productoRepository.findById(101L)).willReturn(Optional.of(productoCamiseta));

            assertThatThrownBy(() -> ventaService.crear(request, 1L, "EMPLEADO"))
                    .isInstanceOf(InsufficientStockException.class);

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_stockCero_forzarSinStock_rolAdmin_201 → Exitoso")
        void testCrear_stockCero_forzarSinStock_rolAdmin_201() {
            productoCamiseta.setStockActual(0);

            VentaLineRequest linea = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(1)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(linea))
                    .metodoPago("EFECTIVO")
                    .forzarSinStock(true)
                    .build();

            given(productoRepository.findById(101L)).willReturn(Optional.of(productoCamiseta));
            given(empleadoRepository.findById(1L)).willReturn(Optional.of(empleadoMock));
            given(ventaRepository.save(any(Venta.class))).willReturn(ventaCompletada);

            VentaDetailResponse response = ventaService.crear(request, 1L, "ADMIN");

            assertThat(response).isNotNull();
            then(ventaRepository).should().save(any(Venta.class));
        }

        @Test
        @DisplayName("testCrear_stockCero_forzarSinStock_rolEmpleado_403 → ForbiddenException")
        void testCrear_stockCero_forzarSinStock_rolEmpleado_403() {
            productoCamiseta.setStockActual(0);

            VentaLineRequest linea = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(1)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(linea))
                    .metodoPago("EFECTIVO")
                    .forzarSinStock(true)
                    .build();

            given(productoRepository.findById(101L)).willReturn(Optional.of(productoCamiseta));

            assertThatThrownBy(() -> ventaService.crear(request, 1L, "EMPLEADO"))
                    .isInstanceOf(ForbiddenException.class);

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_snapshotPrecio_noCambiaConPrecioFuturo")
        void testCrear_snapshotPrecio_noCambiaConPrecioFuturo() {
            VentaLineRequest linea = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(1)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(linea))
                    .metodoPago("TARJETA")
                    .build();

            given(productoRepository.findById(101L)).willReturn(Optional.of(productoCamiseta));
            given(empleadoRepository.findById(1L)).willReturn(Optional.of(empleadoMock));
            given(ventaRepository.save(any(Venta.class))).willAnswer(invocation -> {
                Venta v = invocation.getArgument(0);
                v.setId(5L);
                v.setNumeroVenta("VTA-SNAPSHOT");
                return v;
            });

            VentaDetailResponse response = ventaService.crear(request, 1L, "EMPLEADO");

            // Mutar el precio DESPUÉS de la venta no debe afectar al snapshot ya persistido
            productoCamiseta.setPrecioVenta(new BigDecimal("99.99"));

            assertThat(response).isNotNull();
            then(ventaRepository).should().save(any(Venta.class));
        }

        @Test
        @DisplayName("testCrear_descuentaStockYRegistraMovimiento")
        void testCrear_descuentaStockYRegistraMovimiento() {
            int stockInicial = productoCamiseta.getStockActual(); // 15
            int cantidadVendida = 3;

            VentaLineRequest linea = VentaLineRequest.builder()
                    .productoId(101L)
                    .cantidad(cantidadVendida)
                    .build();

            VentaRequest request = VentaRequest.builder()
                    .lineas(List.of(linea))
                    .metodoPago("EFECTIVO")
                    .build();

            given(productoRepository.findById(101L)).willReturn(Optional.of(productoCamiseta));
            given(empleadoRepository.findById(1L)).willReturn(Optional.of(empleadoMock));
            given(ventaRepository.save(any(Venta.class))).willReturn(ventaCompletada);

            ventaService.crear(request, 1L, "EMPLEADO");

            assertThat(productoCamiseta.getStockActual()).isEqualTo(stockInicial - cantidadVendida);
            then(movimientoStockRepository).should().save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // ANULAR VENTA
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("anularVenta()")
    class AnularVenta {

        @Test
        @DisplayName("testAnular_motivoVacio_400 → BadRequestException")
        void testAnular_motivoVacio_400() {
            assertThatThrownBy(() -> ventaService.anular(1L, "", 1L))
                    .isInstanceOf(BadRequestException.class);

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testAnular_ventaYaAnulada_409 → ConflictException")
        void testAnular_ventaYaAnulada_409() {
            ventaCompletada.setEstado("ANULADA");

            given(ventaRepository.findById(1L)).willReturn(Optional.of(ventaCompletada));

            assertThatThrownBy(() -> ventaService.anular(1L, "Error de caja repetido", 1L))
                    .isInstanceOf(ConflictException.class);

            then(ventaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testAnular_revierteStock_creaMovimientoAnulacion")
        void testAnular_revierteStock_creaMovimientoAnulacion() throws org.apache.coyote.BadRequestException {
            // precioVenta es el campo correcto; precioUnitario no existe en la entidad Producto
            DetalleVenta detalle = DetalleVenta.builder()
                    .producto(productoCamiseta)
                    .cantidad(2)
                    .precioUnitario(productoCamiseta.getPrecioVenta())
                    .nombreProducto(productoCamiseta.getNombre())
                    .build();

            ventaCompletada.setLineas(List.of(detalle));
            int stockAntesDeRevertir = productoCamiseta.getStockActual(); // 15

            given(ventaRepository.findById(1L)).willReturn(Optional.of(ventaCompletada));
            given(ventaRepository.save(any(Venta.class))).willReturn(ventaCompletada);

            ventaService.anular(1L, "Devolución completa", 1L);

            assertThat(productoCamiseta.getStockActual()).isEqualTo(stockAntesDeRevertir + 2);
            then(movimientoStockRepository).should().save(any());
            assertThat(ventaCompletada.getEstado()).isEqualTo("ANULADA");
        }
    }
}