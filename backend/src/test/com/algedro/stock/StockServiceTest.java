package com.algedro.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StockService - Fase 5 RED")
class StockServiceTest {

    @Nested
    @DisplayName("Ajustes manuales")
    class Ajustes {

        @Test
        @DisplayName("testAjuste_sinMotivo_400")
        void testAjuste_sinMotivo_400() throws Exception {
            Object service = newStockService();
            Object request = newStockAjusteRequest(-5, null, false);

            assertThatThrownBy(() -> invoke(service, "registrarAjuste", new Class<?>[]{
                    Long.class,
                    classFor("com.algedro.stock.dto.StockAjusteRequest")
            }, 1L, request))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("El motivo del ajuste es obligatorio");
        }

        @Test
        @DisplayName("testAjuste_motivoMenosDe10Chars_400")
        void testAjuste_motivoMenosDe10Chars_400() throws Exception {
            Object service = newStockService();
            Object request = newStockAjusteRequest(-5, "rotura", false);

            assertThatThrownBy(() -> invoke(service, "registrarAjuste", new Class<?>[]{
                    Long.class,
                    classFor("com.algedro.stock.dto.StockAjusteRequest")
            }, 1L, request))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("El motivo del ajuste debe tener al menos 10 caracteres");
        }

        @Test
        @DisplayName("testStock_nuncaNegativo_sinConfirmacion")
        void testStock_nuncaNegativo_sinConfirmacion() throws Exception {
            Object service = newStockService();
            Object request = newStockAjusteRequest(-100, "regularizacion inventario", false);

            assertThatThrownBy(() -> invoke(service, "registrarAjuste", new Class<?>[]{
                    Long.class,
                    classFor("com.algedro.stock.dto.StockAjusteRequest")
            }, 1L, request))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("El ajuste dejaria el stock en negativo");
        }
    }

    @Nested
    @DisplayName("Movimientos")
    class Movimientos {

        @Test
        @DisplayName("testEntrada_actualizaStockActual")
        void testEntrada_actualizaStockActual() throws Exception {
            Object service = newStockService();
            Object request = newStockEntradaRequest(12, 1L, "ALB-2026-001");

            Object response = invoke(service, "registrarEntrada", new Class<?>[]{
                    Long.class,
                    classFor("com.algedro.stock.dto.StockEntradaRequest")
            }, 1L, request);

            assertThat(read(response, "stockResultante")).isEqualTo(60);
            assertThat(read(response, "cantidad")).isEqualTo(12);
            assertThat(read(response, "tipo")).isEqualTo("ENTRADA");
        }

        @Test
        @DisplayName("testDescuentoAutomatico_porVenta_stub")
        void testDescuentoAutomatico_porVenta_stub() throws Exception {
            Object service = newStockService();

            Object response = invoke(service, "descontarPorVenta", new Class<?>[]{
                    Long.class,
                    Integer.class,
                    Long.class
            }, 1L, 3, 10L);

            assertThat(read(response, "tipo")).isEqualTo("VENTA");
            assertThat(read(response, "cantidad")).isEqualTo(-3);
            assertThat(read(response, "stockResultante")).isEqualTo(45);
        }

        @Test
        @DisplayName("testMovimientoStock_esInmutable")
        void testMovimientoStock_esInmutable() throws Exception {
            Class<?> movimiento = classFor("com.algedro.stock.entity.MovimientoStock");

            assertThat(movimiento.getDeclaredMethods())
                    .filteredOn(method -> method.getName().startsWith("set"))
                    .as("MovimientoStock debe ser inmutable tras persistirse")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("listarStock soporta filtro de alertas")
        void listarStock_soportaFiltroDeAlertas() throws Exception {
            Object service = newStockService();

            Object page = invoke(service, "listarStock", new Class<?>[]{
                    Boolean.class,
                    Long.class,
                    Long.class,
                    org.springframework.data.domain.Pageable.class
            }, true, null, null, PageRequest.of(0, 25));

            assertThat(page).isNotNull();
        }
    }

    private Object newStockService() throws Exception {
        Class<?> serviceClass = classFor("com.algedro.stock.service.StockService");
        Constructor<?> constructor = serviceClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Object newStockEntradaRequest(Integer cantidad, Long proveedorId, String albaran) throws Exception {
        Class<?> requestClass = classFor("com.algedro.stock.dto.StockEntradaRequest");
        return requestClass.getDeclaredConstructor(Integer.class, Long.class, String.class)
                .newInstance(cantidad, proveedorId, albaran);
    }

    private Object newStockAjusteRequest(Integer cantidad, String motivo, Boolean forzarNegativo) throws Exception {
        Class<?> requestClass = classFor("com.algedro.stock.dto.StockAjusteRequest");
        return requestClass.getDeclaredConstructor(Integer.class, String.class, Boolean.class)
                .newInstance(cantidad, motivo, forzarNegativo);
    }

    private Class<?> classFor(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private Object read(Object target, String property) throws Exception {
        Method method = target.getClass().getMethod(property);
        return method.invoke(target);
    }
}
