package com.algedro.stock.repository;

import com.algedro.producto.entity.Producto;
import com.algedro.stock.entity.MovimientoStock;
import com.algedro.stock.entity.TipoMovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    boolean existsByProductoId(Long productoId);

    @Query("""
            SELECT m FROM MovimientoStock m
            WHERE m.producto.id = :productoId
              AND (:tipo IS NULL OR m.tipo = :tipo)
            ORDER BY m.fecha DESC
            """)
    Page<MovimientoStock> buscarHistorial(
            @Param("productoId") Long productoId,
            @Param("tipo") TipoMovimientoStock tipo,
            Pageable pageable
    );
    /**
     * Encuentra el historial de movimientos de inventario de una venta/anulación específica.
     */
    List<MovimientoStock> findByVentaId(Long ventaId);
}
