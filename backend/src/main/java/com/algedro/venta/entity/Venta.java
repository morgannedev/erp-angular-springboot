package com.algedro.venta.entity;

import com.algedro.cliente.entity.Cliente;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas", indexes = {
    @Index(name = "idx_ventas_numero_venta", columnList = "numero_venta", unique = true),
    @Index(name = "idx_ventas_empleado_id", columnList = "empleado_id"),
    @Index(name = "idx_ventas_cliente_id", columnList = "cliente_id"),
    @Index(name = "idx_ventas_fecha", columnList = "fecha"),
    @Index(name = "idx_ventas_estado", columnList = "estado"),
    @Index(name = "idx_ventas_fecha_estado", columnList = "fecha, estado")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_venta", nullable = false, unique = true, length = 20)
    private String numeroVenta;

    @Column(name = "empleado_id", nullable = false)
    private Long empleadoId; // Enlazado al ID de la tabla de empleados

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // Relación opcional con cliente (NULLABLE)

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento_global", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuentoGlobal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago; // 'EFECTIVO', 'TARJETA', 'OTRO'

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "COMPLETADA"; // 'COMPLETADA' o 'ANULADA'

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @Column(name = "anulada_por")
    private Long anuladaPor; // Empleado ID administrativo que ejecuta la baja

    @Column(name = "fecha_anulacion", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime fechaAnulacion;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;

    // Relación compositiva fuerte: si se borra la cabecera, mueren sus líneas
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleVenta> lineas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.fecha == null) {
            this.fecha = OffsetDateTime.now();
        }
        if (this.descuentoGlobal == null) {
            this.descuentoGlobal = BigDecimal.ZERO;
        }
        if (this.estado == null) {
            this.estado = "COMPLETADA";
        }
    }

    /**
     * Helper metod para sincronizar de manera bidireccional las líneas
     */
    public void addLinea(DetalleVenta detalle) {
        lineas.add(detalle);
        detalle.setVenta(this);
    }
}