package com.algedro.producto.entity;

import com.algedro.categoria.entity.Categoria;
import com.algedro.proveedor.entity.Proveedor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String referencia; // SKU único

    @Column(unique = true, length = 14)
    private String ean; // Código de barras EAN único (nullable por defecto)

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor; // Nullable según el esquema

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta; // PVP — visible para todos los roles

    @Column(name = "precio_coste")
    private BigDecimal precioCoste; // Omitido en la respuesta para el rol EMPLEADO

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida; // Ejemplo: "ud"

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual; // Campo operativo de consulta rápida

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "stock_maximo")
    private Integer stockMaximo;

    @Column(name = "en_alerta", nullable = false)
    private Boolean enAlerta; // true si stockActual < stockMinimo

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Ciclos de vida de JPA para auditoría básica y lógica de negocio ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
        this.evaluarAlertaStock();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.evaluarAlertaStock();
    }

    private void evaluarAlertaStock() {
        if (this.stockActual != null && this.stockMinimo != null) {
            this.enAlerta = this.stockActual < this.stockMinimo;
        } else {
            this.enAlerta = false;
        }
    }

    public BigDecimal getPrecioUnitario() {
        return this.precioVenta;
    }
}