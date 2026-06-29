package com.algedro.proveedor.repository;

import com.algedro.proveedor.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** Unicidad de NIF — usado en ProveedorService antes de save() */
    Optional<Proveedor> findByNif(String nif);

    /** Listado paginado con filtro opcional de nombre y estado activo */
    @Query(value = """
    SELECT p.* FROM proveedores p
    WHERE (:q IS NULL OR p.nombre ILIKE CONCAT('%', :q, '%'))
    AND (:activo IS NULL OR p.activo = :activo)
    ORDER BY p.nombre
    """,
            countQuery = """
    SELECT COUNT(*) FROM proveedores p
    WHERE (:q IS NULL OR p.nombre ILIKE CONCAT('%', :q, '%'))
    AND (:activo IS NULL OR p.activo = :activo)
    """,
            nativeQuery = true)
    Page<Proveedor> buscar(
            @Param("q") String q,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}