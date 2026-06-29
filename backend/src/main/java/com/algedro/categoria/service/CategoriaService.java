package com.algedro.categoria.service;

import com.algedro.categoria.dto.CategoriaRequestDTO;
import com.algedro.categoria.dto.CategoriaArbolDTO;
import com.algedro.categoria.dto.CategoriaResponseDTO;
import com.algedro.categoria.dto.CategoriaResumenDTO;
import com.algedro.categoria.entity.Categoria;
import com.algedro.categoria.repository.CategoriaRepository;
import com.algedro.exception.AutoReferenciaException;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.NivelMaximoExcedidoException;
import com.algedro.exception.RecursoNoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CategoriaService — implementación GREEN para la Fase 3.
 *
 * Reglas de negocio (A7 / Data-Model.md §1.4 / openapi.yaml):
 *
 *  R1 — NIVEL MÁXIMO:
 *       Solo se admite 1 nivel de profundidad.
 *       padre=null       → nivel 0 (raíz)   ✔
 *       padre.padre=null → nivel 1 (sub)    ✔
 *       padre.padre≠null → nivel 2 (sub-sub) ✘ → NivelMaximoExcedidoException
 *
 *  R2 — AUTO-REFERENCIA:
 *       padre_id no puede ser igual al propio id.
 *       Refleja chk_categorias_no_self_ref de la BD, pero se valida antes.
 *       → AutoReferenciaException
 *
 *  R3 — HAPPY PATH:
 *       Crear subcategoría de una raíz → persiste correctamente.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // ─────────────────────────────────────────────────────────────
    // crear — usado para nuevas categorías (id propio desconocido)
    // ─────────────────────────────────────────────────────────────

    public Categoria crear(CategoriaRequestDTO dto) {
        return crear(dto, null);
    }

    public CategoriaResponseDTO crearResponse(CategoriaRequestDTO dto) {
        return toResponse(crear(dto));
    }

    /**
     * Crear categoría con id propio explícito (usado en actualizaciones
     * y en el test de auto-referencia donde el id ya es conocido).
     *
     * @param dto      datos de la categoría
     * @param idPropio id de la entidad en curso (null si es nueva)
     */
    public Categoria crear(CategoriaRequestDTO dto, Long idPropio) {
        Categoria padre = resolverPadre(dto.padreId());

        // R2 — Auto-referencia: el padre no puede ser la propia entidad
        if (idPropio != null && dto.padreId() != null
                && dto.padreId().equals(idPropio)) {
            throw new AutoReferenciaException(idPropio);
        }

        // R1 — Nivel máximo: si el padre ya tiene padre → es nivel 1 → hijo sería nivel 2
        if (padre != null && !padre.esRaiz()) {
            throw new NivelMaximoExcedidoException();
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(dto.nombre());
        categoria.setDescripcion(dto.descripcion());
        categoria.setPadre(padre);
        categoria.setActivo(true);

        return categoriaRepository.save(categoria);
    }

    @Transactional(readOnly = true)
    public List<CategoriaArbolDTO> listarArbol(boolean soloActivas) {
        List<Categoria> raices = categoriaRepository.findRaices(soloActivas);
        Map<Long, List<CategoriaResponseDTO>> subsPorPadre = raices.stream()
                .collect(Collectors.toMap(
                        Categoria::getId,
                        raiz -> categoriaRepository.findByPadreId(raiz.getId()).stream()
                                .filter(c -> !soloActivas || c.isActivo())
                                .map(this::toResponse)
                                .toList()
                ));

        return raices.stream()
                .map(c -> new CategoriaArbolDTO(
                        c.getId(),
                        c.getNombre(),
                        c.getDescripcion(),
                        c.isActivo(),
                        subsPorPadre.getOrDefault(c.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResumenDTO> listarResumen() {
        return categoriaRepository.findRaices(true).stream()
                .map(c -> new CategoriaResumenDTO(c.getId(), c.getNombre()))
                .toList();
    }

    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO dto) {
        Categoria existente = buscarPorId(id);
        Categoria padre = resolverPadre(dto.padreId());

        if (dto.padreId() != null && dto.padreId().equals(id)) {
            throw new AutoReferenciaException(id);
        }
        if (padre != null && !padre.esRaiz()) {
            throw new NivelMaximoExcedidoException();
        }

        existente.setNombre(dto.nombre());
        existente.setDescripcion(dto.descripcion());
        existente.setPadre(padre);
        return toResponse(categoriaRepository.save(existente));
    }

    public CategoriaResponseDTO cambiarEstado(Long id, boolean activo) {
        Categoria categoria = buscarPorId(id);
        categoria.setActivo(activo);
        return toResponse(categoriaRepository.save(categoria));
    }

    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("No se puede eliminar la categoria porque tiene productos o subcategorias asociadas");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────

    /**
     * Resuelve el padre si padreId no es null.
     * @throws RecursoNoEncontradoException si el padreId no existe en BD.
     */
    private Categoria resolverPadre(Long padreId) {
        if (padreId == null) {
            return null; // raíz
        }
        return categoriaRepository.findById(padreId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría padre no encontrada con id=" + padreId
                ));
    }

    private Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada con id=" + id));
    }

    private CategoriaResponseDTO toResponse(Categoria c) {
        Categoria padre = c.getPadre();
        return new CategoriaResponseDTO(
                c.getId(),
                c.getNombre(),
                c.getDescripcion(),
                padre != null ? padre.getId() : null,
                padre != null ? padre.getNombre() : null,
                c.isActivo(),
                c.getCreatedAt()
        );
    }
}
