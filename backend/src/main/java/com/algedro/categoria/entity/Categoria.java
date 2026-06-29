package com.algedro.categoria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad JPA — tabla `categorias`
 * Data-Model.md §1.4
 *
 * Jerarquía máxima de 2 niveles (validada en CategoriaService, no en BD — A7).
 * Constraint de BD: chk_categorias_no_self_ref → padre_id IS NULL OR padre_id <> id
 */
@Entity
@Table(name = "categorias")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /**
     * NULL → categoría raíz (nivel 0).
     * NOT NULL → subcategoría (nivel 1).
     * El nivel 2 está bloqueado en CategoriaService.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private Categoria padre;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Devuelve true si esta categoría es raíz (sin padre) */
    public boolean esRaiz() {
        return padre == null;
    }
}