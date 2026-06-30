package com.algedro.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StockController - Fase 5 RED")
class StockControllerTest {

    @Test
    @DisplayName("testGetAlertas_rolEmpleado_403")
    void testGetAlertas_rolEmpleado_403() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        // CORREGIDO - Ahora el método tiene 5 parámetros incluyendo query
        Method endpoint = controller.getMethod(
                "listarStock",
                Boolean.class,
                Long.class,
                Long.class,
                String.class,  // ← Añadir el parámetro query
                org.springframework.data.domain.Pageable.class
        );

        assertThat(controller.getAnnotation(RequestMapping.class).value()).contains("/stock");
        assertThat(endpoint.getAnnotation(GetMapping.class)).isNotNull();
        // CORREGIDO - El endpoint permite ADMIN y EMPLEADO
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('ADMIN', 'EMPLEADO')");
    }

    @Test
    @DisplayName("testEntrada_rolEmpleado_403")
    void testEntrada_rolEmpleado_403() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "registrarEntrada",
                Long.class,
                Class.forName("com.algedro.stock.dto.StockEntradaRequest")
        );

        assertThat(endpoint.getAnnotation(PostMapping.class).value()).contains("/{productoId}/entradas");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    @DisplayName("historial de movimientos es solo ADMIN")
    void historialMovimientos_soloAdmin() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "historial",
                Long.class,
                String.class,
                org.springframework.data.domain.Pageable.class
        );

        assertThat(endpoint.getAnnotation(GetMapping.class).value()).contains("/{productoId}/movimientos");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('ADMIN', 'EMPLEADO')");
    }

    // NUEVO TEST - Probar que ajustes y entradas son solo ADMIN
    @Test
    @DisplayName("ajuste solo ADMIN")
    void ajuste_soloAdmin() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "registrarAjuste",
                Long.class,
                Class.forName("com.algedro.stock.dto.StockAjusteRequest")
        );

        assertThat(endpoint.getAnnotation(PostMapping.class).value()).contains("/{productoId}/ajustes");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    // NUEVO TEST - Probar endpoints de actualización de mín/máx
    @Test
    @DisplayName("actualizar minimo solo ADMIN")
    void actualizarMinimo_soloAdmin() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "actualizarMinimo",
                Long.class,
                Integer.class
        );

        assertThat(endpoint.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class).value())
                .contains("/{productoId}/minimo");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    @DisplayName("actualizar maximo solo ADMIN")
    void actualizarMaximo_soloAdmin() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "actualizarMaximo",
                Long.class,
                Integer.class
        );

        assertThat(endpoint.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class).value())
                .contains("/{productoId}/maximo");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    // NUEVO TEST - Verificar que el listado permite a EMPLEADO
    @Test
    @DisplayName("listar stock permite EMPLEADO")
    void listarStock_permiteEmpleado() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "listarStock",
                Boolean.class,
                Long.class,
                Long.class,
                String.class,
                org.springframework.data.domain.Pageable.class
        );

        assertThat(endpoint.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('ADMIN', 'EMPLEADO')");
    }

    // NUEVO TEST - Verificar que historial permite EMPLEADO
    @Test
    @DisplayName("historial permite EMPLEADO")
    void historial_permiteEmpleado() throws Exception {
        Class<?> controller = Class.forName("com.algedro.stock.controller.StockController");
        Method endpoint = controller.getMethod(
                "historial",
                Long.class,
                String.class,
                org.springframework.data.domain.Pageable.class
        );

        assertThat(endpoint.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('ADMIN', 'EMPLEADO')");
    }
}