package com.algedro.proveedor.dto;

import java.time.LocalDateTime;

/**
 * ProveedorResponseDTO — mapea el schema Proveedor de openapi.yaml.
 * Se devuelve para ADMIN y EMPLEADO (misma respuesta, solo lectura para EMPLEADO).
 */
public record ProveedorResponseDTO(
        Long          id,
        String        nombre,
        String        nif,
        String        contactoNombre,
        String        telefono,
        String        email,
        String        direccion,
        String        ciudad,
        boolean       activo,
        String        notas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}