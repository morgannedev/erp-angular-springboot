package com.algedro.stock.entity;

import com.algedro.empleado.entity.Empleado;
import com.algedro.producto.entity.Producto;
import com.algedro.proveedor.entity.Proveedor;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoStock tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "venta_id")
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(length = 100)
    private String albaran;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    public MovimientoStock(
            Producto producto,
            TipoMovimientoStock tipo,
            Integer cantidad,
            Integer stockResultante,
            String motivo,
            Long ventaId,
            Proveedor proveedor,
            String albaran,
            Empleado empleado
    ) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.motivo = motivo;
        this.ventaId = ventaId;
        this.proveedor = proveedor;
        this.albaran = albaran;
        this.empleado = empleado;
    }

    public void asociarVenta(Long ventaId) {
        this.ventaId = ventaId;
    }

    @PrePersist
    void onCreate() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }
}
