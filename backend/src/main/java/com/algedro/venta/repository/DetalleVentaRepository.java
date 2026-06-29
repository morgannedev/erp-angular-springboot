package com.algedro.venta.repository;

import com.algedro.venta.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    @Query(value = "SELECT COUNT(*) > 0 FROM detalle_ventas WHERE producto_id = :productoId", nativeQuery = true)
    boolean existsByProductoId(@Param("productoId") Long productoId);
    /**
     * Recupera todas las líneas de una venta específica.
     */
    List<DetalleVenta> findByVentaId(Long ventaId);
}
