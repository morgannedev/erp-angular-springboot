package com.algedro.proveedor;

import com.algedro.exception.RecursoNoEncontradoException;
import com.algedro.exception.ConflictoException;
import com.algedro.proveedor.dto.ProveedorRequestDTO;
import com.algedro.proveedor.dto.ProveedorResponseDTO;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.proveedor.repository.ProveedorRepository;
import com.algedro.proveedor.service.ProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Fase 3 — ProveedorServiceTest
 *
 * Fuentes de verdad:
 *   - Data-Model.md §1.3  → columna nif UNIQUE NULL, activo BOOLEAN DEFAULT TRUE
 *   - openapi.yaml        → POST /proveedores: 201 | 409 (NIF duplicado)
 *                         → DELETE /proveedores/{id}: soft-delete recomendado
 *   - technical-design.md → A7: validación en Service, no en BD
 *
 * Ciclo TDD:
 *   RED   → este fichero (no compila hasta crear los stubs)
 *   GREEN → ProveedorService.java con lógica mínima que hace pasar cada test
 *   REFACTOR → ver comentarios inline
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProveedorService — Fase 3")
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    // ──────────────────────────────────────────────────────────────
    // Fixtures
    // ──────────────────────────────────────────────────────────────

    private ProveedorRequestDTO requestValido;

    @BeforeEach
    void setUp() {
        // Coincide con el seed de Data-Model.md §4
        requestValido = new ProveedorRequestDTO(
                "Distribuciones Químicas del Sur S.L.",
                "B12345678",          // nif
                "Pedro Martínez",     // contactoNombre
                "954111222",
                "pedidos@dqsur.es",
                "Calle Ejemplo, 1",
                "Sevilla",
                null                  // notas
        );
    }

    // ══════════════════════════════════════════════════════════════
    // testCrear_nifDuplicado_409
    //
    // Regla de negocio: el NIF de proveedor es UNIQUE en BD
    // (Data-Model.md §1.3). El Service debe detectarlo ANTES de
    // llegar a la BD y lanzar ConflictoException → GlobalExceptionHandler
    // lo traduce a HTTP 409.
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear → NIF ya existe en BD → lanza ConflictoException con el NIF en el mensaje")
    void testCrear_nifDuplicado_409() {
        // Arrange: la BD ya tiene un proveedor con ese NIF
        given(proveedorRepository.findByNif("B12345678"))
                .willReturn(Optional.of(proveedorConId(1L)));

        // Act & Assert
        assertThatThrownBy(() -> proveedorService.crear(requestValido))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("B12345678");

        // El service NO debe llegar a llamar a save()
        then(proveedorRepository).should(never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // testSoftDelete_exitoso
    //
    // Regla de negocio (openapi.yaml §DELETE /proveedores/{id}):
    //   "Se recomienda desactivar en lugar de eliminar".
    //   Los productos vinculados quedan con proveedor_id = NULL
    //   (ON DELETE SET NULL, Data-Model.md §5.5).
    //
    // La implementación de softDelete debe:
    //   1. Buscar el proveedor → 404 si no existe
    //   2. Poner activo = false
    //   3. save() → jamás deleteById()
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("softDelete → proveedor activo existente → persiste con activo=false, nunca deleteById")
    void testSoftDelete_exitoso() {
        // Arrange
        Long id = 1L;
        Proveedor proveedor = proveedorConId(id);
        proveedor.setActivo(true);

        given(proveedorRepository.findById(id)).willReturn(Optional.of(proveedor));
        given(proveedorRepository.save(any(Proveedor.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        proveedorService.softDelete(id);

        // Assert 1: se llamó a save() con activo=false
        then(proveedorRepository).should().save(argThat(p -> !p.isActivo()));

        // Assert 2: jamás se eliminó físicamente el registro
        then(proveedorRepository).should(never()).deleteById(any());
        then(proveedorRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("softDelete → proveedor inexistente → lanza RecursoNoEncontradoException")
    void testSoftDelete_noExiste_404() {
        given(proveedorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorService.softDelete(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        then(proveedorRepository).should(never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private Proveedor proveedorConId(Long id) {
        Proveedor p = new Proveedor();
        p.setId(id);
        p.setNombre("Proveedor Test");
        p.setNif("B12345678");
        p.setActivo(true);
        return p;
    }
}