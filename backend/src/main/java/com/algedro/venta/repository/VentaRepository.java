package com.algedro.venta.repository;

import com.algedro.venta.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    long count();

    boolean existsByClienteId(Long clienteId);

    @Query("SELECT v FROM Venta v WHERE " +
            "v.cliente.id = :clienteId AND " +
            "(:desde IS NULL OR v.fecha >= :desde) AND " +
            "(:hasta IS NULL OR v.fecha <= :hasta)")
    Page<Venta> findByClienteIdAndFechaBetween(
            Long clienteId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Pageable pageable
    );

    /**
     * Consulta con filtrado dinámico multi-criterio para el historial del POS.
     * Usando SQL nativo para evitar problemas de tipos con PostgreSQL.
     */
    @Query(value = """
    SELECT * FROM ventas v 
    WHERE (v.fecha >= COALESCE(cast(:desde as timestamp), v.fecha))
      AND (v.fecha <= COALESCE(cast(:hasta as timestamp), v.fecha))
      AND (v.empleado_id = COALESCE(cast(:empleadoId as bigint), v.empleado_id))
      AND (v.cliente_id = COALESCE(cast(:clienteId as bigint), v.cliente_id))
      AND (v.metodo_pago = COALESCE(cast(:metodoPago as text), v.metodo_pago))
      AND (v.estado = COALESCE(cast(:estado as text), v.estado))
    ORDER BY v.fecha DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM ventas v 
    WHERE (v.fecha >= COALESCE(cast(:desde as timestamp), v.fecha))
      AND (v.fecha <= COALESCE(cast(:hasta as timestamp), v.fecha))
      AND (v.empleado_id = COALESCE(cast(:empleadoId as bigint), v.empleado_id))
      AND (v.cliente_id = COALESCE(cast(:clienteId as bigint), v.cliente_id))
      AND (v.metodo_pago = COALESCE(cast(:metodoPago as text), v.metodo_pago))
      AND (v.estado = COALESCE(cast(:estado as text), v.estado))
    """,
            nativeQuery = true)
    Page<Venta> buscarVentasFiltradas(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("empleadoId") Long empleadoId,
            @Param("clienteId") Long clienteId,
            @Param("metodoPago") String metodoPago,
            @Param("estado") String estado,
            Pageable pageable
    );

    /**
     * Recupera el listado plano completo según rango de fechas para exportaciones CSV (solo ADMIN).
     */
    @Query("SELECT v FROM Venta v WHERE " +
            "(:desde IS NULL OR v.fecha >= :desde) AND " +
            "(:hasta IS NULL OR v.fecha <= :hasta) " +
            "ORDER BY v.fecha DESC")
    List<Venta> findByFechaBetweenParaExportar(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta
    );
}