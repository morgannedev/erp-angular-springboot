package com.algedro.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CategoriaRequestDTO — mapea CategoriaRequest de openapi.yaml.
 * padreId nullable: si se omite → raíz; si se proporciona → subcategoría.
 */
public record CategoriaRequestDTO(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255)           String descripcion,
                                   Long   padreId     // null = raíz
) {}