package com.algedro.categoria.dto;

import java.time.LocalDateTime;

public record CategoriaResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Long padreId,
        String padreNombre,
        boolean activo,
        LocalDateTime createdAt
) {}
