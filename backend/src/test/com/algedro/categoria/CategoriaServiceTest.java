package com.algedro.categoria;

import com.algedro.categoria.entity.Categoria;
import com.algedro.categoria.repository.CategoriaRepository;
import com.algedro.categoria.service.CategoriaService;
import com.algedro.exception.AutoReferenciaException;
import com.algedro.exception.NivelMaximoExcedidoException;
import com.algedro.exception.RecursoNoEncontradoException;
import com.algedro.categoria.dto.CategoriaRequestDTO;
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
 * Fase 3 — CategoriaServiceTest
 *
 * Fuentes de verdad:
 *   - Data-Model.md §1.4  → padre_id FK auto-ref; constraint chk_categorias_no_self_ref
 *                           "La restricción de máximo dos niveles se aplica a nivel de
 *                            aplicación (Spring Boot), no en BD" (§1.4 Nota)
 *   - openapi.yaml        → POST /categorias: 201 | 409 (excede dos niveles)
 *                           CategoriaRequest: padreId nullable; "Solo se admite un nivel
 *                           de profundidad (subcategoría de una categoría raíz)"
 *   - technical-design.md → A7: "Validación de jerarquía de categorías en Service, no en BD"
 *
 * Árbol de jerarquía permitida (máximo 2 niveles):
 *   Nivel 0 (raíz) → padre_id = NULL
 *   Nivel 1 (sub)  → padre_id = id de raíz   ← máximo permitido
 *   Nivel 2 (sub-sub) → BLOQUEADO en Service
 *
 * Ejemplo del seed (Data-Model.md §4):
 *   'Limpieza del Hogar'  padre_id=NULL  → nivel 0
 *   'Limpieza de Suelos'  padre_id=1     → nivel 1  ✔
 *   Cualquier hijo de 'Limpieza de Suelos' → nivel 2  ✘
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService — Fase 3")
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    // ──────────────────────────────────────────────────────────────
    // Fixtures (replicando el seed de Data-Model.md §4)
    // ──────────────────────────────────────────────────────────────

    private Categoria raiz;   // id=1, padre=null  → nivel 0
    private Categoria nivel1; // id=3, padre=raiz  → nivel 1

    @BeforeEach
    void setUp() {
        raiz = new Categoria();
        raiz.setId(1L);
        raiz.setNombre("Limpieza del Hogar");
        raiz.setPadre(null);
        raiz.setActivo(true);

        nivel1 = new Categoria();
        nivel1.setId(3L);
        nivel1.setNombre("Limpieza de Suelos");
        nivel1.setPadre(raiz);   // nivel 1 — ya es subcategoría
        nivel1.setActivo(true);
    }

    // ══════════════════════════════════════════════════════════════
    // testCrear_subcategoria_exitoso
    //
    // Happy path: crear subcategoría de una categoría raíz (nivel 0→1).
    // Resultado esperado: categoría guardada con padre = raiz.
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear → padre en nivel 0 (raíz) → crea subcategoría nivel 1 y la persiste")
    void testCrear_subcategoria_exitoso() {
        // Arrange
        CategoriaRequestDTO dto = new CategoriaRequestDTO(
                "Desinfectantes",     // nombre
                "Lejías y amoniaco",  // descripcion
                1L                    // padreId → raíz
        );
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(raiz));
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        // Act
        Categoria resultado = categoriaService.crear(dto);

        // Assert
        assertThat(resultado.getPadre()).isEqualTo(raiz);
        assertThat(resultado.getNombre()).isEqualTo("Desinfectantes");
        assertThat(resultado.isActivo()).isTrue(); // activo por defecto
        then(categoriaRepository).should().save(any(Categoria.class));
    }

    // ══════════════════════════════════════════════════════════════
    // testCrear_tercerNivel_lanzaExcepcion
    //
    // Regla de negocio (A7 / openapi.yaml):
    //   "Solo se admite un nivel de profundidad".
    //   nivel1 ya tiene padre → sus hijos serían nivel 2 → PROHIBIDO.
    //   El Service lanza NivelMaximoExcedidoException antes de save().
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear → padre ya es nivel 1 → lanza NivelMaximoExcedidoException, sin persistencia")
    void testCrear_tercerNivel_lanzaExcepcion() {
        // Arrange: intentamos crear un hijo de nivel1 (que ya tiene padre)
        CategoriaRequestDTO dto = new CategoriaRequestDTO(
                "Sub-Sub Inválida",
                null,
                3L    // padreId → nivel1 (que ya tiene padre=raiz)
        );
        given(categoriaRepository.findById(3L)).willReturn(Optional.of(nivel1));

        // Act & Assert
        assertThatThrownBy(() -> categoriaService.crear(dto))
                .isInstanceOf(NivelMaximoExcedidoException.class)
                .hasMessageContaining("nivel");

        // Garantía: nunca se tocó la BD para guardar
        then(categoriaRepository).should(never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // testCrear_autoReferencia_lanzaExcepcion
    //
    // Regla de negocio (Data-Model.md §1.4 constraint):
    //   chk_categorias_no_self_ref → padre_id <> id
    //   El Service debe detectarlo antes de llegar a la BD.
    //
    // Escenario: creamos una categoría con id=5 y queremos que su
    // padre también sea id=5 (referencia circular directa).
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear → padreId == id de la misma categoría → lanza AutoReferenciaException")
    void testCrear_autoReferencia_lanzaExcepcion() {
        // Arrange: la categoría "Circular" con id=5 intenta ser su propio padre
        Categoria circular = new Categoria();
        circular.setId(5L);
        circular.setNombre("Circular");
        circular.setPadre(null);
        circular.setActivo(true);

        // El DTO lleva padreId = 5 (la propia categoría)
        CategoriaRequestDTO dto = new CategoriaRequestDTO("Circular", null, 5L);

        // El service primero busca el padre para validarlo
        given(categoriaRepository.findById(5L)).willReturn(Optional.of(circular));

        // Act & Assert
        // NOTA: la autoRef se detecta al actualizar (id ya conocido).
        // Para el caso "nueva categoría": el service detecta que el padre
        // devuelto tiene el mismo nombre/referencia que la entidad en curso,
        // o bien se valida en el update con id fijo. Se usa el overload
        // crear(dto, idPropia) para este test.
        assertThatThrownBy(() -> categoriaService.crear(dto, 5L))
                .isInstanceOf(AutoReferenciaException.class);

        then(categoriaRepository).should(never()).save(any());
    }
}