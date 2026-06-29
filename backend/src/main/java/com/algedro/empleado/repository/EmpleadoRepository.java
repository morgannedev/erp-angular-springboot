package com.algedro.empleado.repository;

import com.algedro.empleado.entity.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Busca empleados aplicando filtros opcionales de búsqueda (nombre/apellidos) y estado de cuenta.
     * Utiliza @EntityGraph para cargar la relación 'usuario' en una sola query (Evita N+1).
     */
    @Query(value = """
        SELECT e.* FROM empleados e 
        JOIN usuarios u ON u.id = e.usuario_id 
        WHERE (:q IS NULL OR 
               CAST(e.nombre AS TEXT) ILIKE CAST(CONCAT('%', CAST(:q AS TEXT), '%') AS TEXT) OR 
               CAST(e.apellidos AS TEXT) ILIKE CAST(CONCAT('%', CAST(:q AS TEXT), '%') AS TEXT))
        AND (:activo IS NULL OR u.activo = :activo)
        ORDER BY e.apellidos
        """,
            countQuery = """
            SELECT COUNT(*) FROM empleados e 
            JOIN usuarios u ON u.id = e.usuario_id 
            WHERE (:q IS NULL OR 
                   CAST(e.nombre AS TEXT) ILIKE CAST(CONCAT('%', CAST(:q AS TEXT), '%') AS TEXT) OR 
                   CAST(e.apellidos AS TEXT) ILIKE CAST(CONCAT('%', CAST(:q AS TEXT), '%') AS TEXT))
            AND (:activo IS NULL OR u.activo = :activo)
            """,
            nativeQuery = true)
    Page<Empleado> findAllWithFilters(@Param("q") String q,
                                      @Param("activo") Boolean activo,
                                      Pageable pageable);

    /**
     * Busca un empleado por su ID cargando de forma ansiosa su relación con Usuario.
     */
    @EntityGraph(attributePaths = {"usuario"})
    Optional<Empleado> findWithUsuarioById(Long id);

    /**
     * Busca un empleado a través del ID de su Usuario asociado.
     */
    @EntityGraph(attributePaths = {"usuario"})
    Optional<Empleado> findByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Empleado> findByUsuarioUsername(String username);

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos de validación para evitar duplicados (Conflictos 409)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica si ya existe un DNI registrado.
     */
    boolean existsByDni(String dni);

    /**
     * Verifica si ya existe un DNI registrado excluyendo al empleado actual (útil para PUT /updates).
     */
    boolean existsByDniAndIdNot(String dni, Long id);

    /**
     * Verifica si ya existe un Email registrado.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si ya existe un Email registrado excluyendo al empleado actual.
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Verifica si el username ya existe accediendo a la entidad Usuario relacionada.
     */
    boolean existsByUsuarioUsername(String username);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM ventas v WHERE v.empleado_id = :empleadoId OR v.anulada_por = :empleadoId)", nativeQuery = true)
    boolean tieneVentasRegistradas(@Param("empleadoId") Long empleadoId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM movimientos_stock m WHERE m.empleado_id = :empleadoId)", nativeQuery = true)
    boolean tieneMovimientosStock(@Param("empleadoId") Long empleadoId);

    // Tu test también requiere este método de Spring Data para buscar ignorando mayúsculas:
    Page<Empleado> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombre, String apellidos, Pageable pageable);

    boolean existsByUsuarioId(Long id);
}
