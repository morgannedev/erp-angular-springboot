package com.algedro.categoria.dto;

import java.util.List;

public record CategoriaArbolDTO(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        List<CategoriaResponseDTO> subcategorias
) {}
