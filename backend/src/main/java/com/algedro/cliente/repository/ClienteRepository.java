package com.algedro.cliente.repository;

import com.algedro.cliente.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Validaciones de unicidad requeridas para la creación (Crear)
    boolean existsByNif(String nif);
    boolean existsByEmail(String email);

    // Validaciones de unicidad requeridas para la edición (Actualizar)
    boolean existsByNifAndIdNot(String nif, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Búsqueda unificada paginada para el POS y paneles de gestión.
     * Evalúa coincidencias parciales e insensibles a mayúsculas/minúsculas en 'nombre' o 'apellidos', 
     * o coincidencias parciales sobre el 'telefono'.
     */
    @Query("SELECT c FROM Cliente c WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(c.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " c.telefono LIKE CONCAT('%', :query, '%')) " +
           "AND c.activo = :activo")
    Page<Cliente> buscar(@Param("query") String query, 
                         @Param("activo") boolean activo, 
                         Pageable pageable);
}