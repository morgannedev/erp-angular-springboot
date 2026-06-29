package com.algedro.venta.entity;

import com.algedro.producto.entity.Producto;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_ventas", indexes = {
    @Index(name = "idx_detalle_ventas_venta_id", columnList = "venta_id"),
    @Index(name = "idx_detalle_ventas_producto_id", columnList = "producto_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta; // Cabecera de la transacción

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto; // Producto origen (para cálculos cruzados)

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario; // Snapshot del precio de catálogo al vender

    @Column(name = "descuento_linea", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuentoLinea = BigDecimal.ZERO;

    @Column(name = "subtotal_linea", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalLinea; // (cantidad * precioUnitario) - descuentoLinea

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto; // Snapshot de auditoría textual del nombre comercial
}