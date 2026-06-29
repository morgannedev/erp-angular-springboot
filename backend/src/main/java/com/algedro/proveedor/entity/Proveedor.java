package com.algedro.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, columnDefinition = "VARCHAR(150)")
    private String nombre;

    @Column(unique = true, length = 20, columnDefinition = "VARCHAR(20)")
    private String nif;

    @Column(name = "contacto_nombre", length = 100, columnDefinition = "VARCHAR(100)")
    private String contactoNombre;

    @Column(length = 20, columnDefinition = "VARCHAR(20)")
    private String telefono;

    @Column(length = 150, columnDefinition = "VARCHAR(150)")
    private String email;

    @Column(length = 255, columnDefinition = "VARCHAR(255)")
    private String direccion;

    @Column(length = 100, columnDefinition = "VARCHAR(100)")
    private String ciudad;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}