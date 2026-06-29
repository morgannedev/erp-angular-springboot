package com.algedro.producto.repository;

import com.algedro.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // ── 4.4: Búsquedas Específicas ───────────────────────────────────────────

    /**
     * Búsqueda por nombre sin distinguir mayúsculas/minúsculas (ILIKE) con soporte para paginación.
     */
    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    /**
     * Busca un producto por su código EAN único.
     */
    Optional<Producto> findByEan(String ean);

    /**
     * Requisito de la sección "BÚSQUEDA POR EAN" del Test:
     * Busca un producto por EAN pero filtra estrictamente que esté activo (activo = true).
     */
    Optional<Producto> findByEanAndActivoTrue(String ean);

    Optional<Producto> findByReferencia(String referencia);

    // ── Métodos de Validación (Requeridos por el ciclo TDD del Test) ────────

    /**
     * Verifica si ya existe un producto con el EAN indicado.
     * Utilizado en la validación antes de crear un nuevo producto.
     */
    boolean existsByEan(String ean);

    /**
     * Verifica si ya existe un producto con la referencia (SKU) indicada.
     * Utilizado en la validación antes de crear un nuevo producto.
     */
    boolean existsByReferencia(String referencia);


    // ── Métodos de Listado Avanzado / Filtrado Dinámico ──────────────────────

    /**
     * Requisito de la sección "PAGINACIÓN Y LISTADO" del Test.
     * Permite buscar productos filtrando dinámicamente por término de búsqueda (nombre/referencia),
     * categoría, proveedor y su estado de actividad.
     * * Nota: En el test se invoca como: buscarActivos(query, catId, provId, activo, pageable)
     */
    @Query("SELECT p FROM Producto p WHERE " +
            "(:query IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.referencia) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
            "(:proveedorId IS NULL OR p.proveedor.id = :proveedorId) AND " +
            "(:activo IS NULL OR p.activo = :activo)")
    Page<Producto> buscarActivos(
            @Param("query") String query,
            @Param("categoriaId") Long categoriaId,
            @Param("proveedorId") Long proveedorId,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    @Query("SELECT p FROM Producto p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.referencia) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Producto> buscarPorNombreOReferencia(@Param("query") String query, Pageable pageable);

    /**
     * Sobrecarga alternativa inferida por la firma del test en la sección "Precio Coste según Rol".
     */
    default Page<Producto> buscarActivos(Long categoriaId, Long proveedorId, String query, Boolean activo, Pageable pageable) {
        return buscarActivos(query, categoriaId, proveedorId, activo, pageable);
    }
}