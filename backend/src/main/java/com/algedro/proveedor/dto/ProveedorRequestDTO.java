package com.algedro.proveedor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ProveedorRequestDTO — mapea ProveedorRequest de openapi.yaml
 * Campo obligatorio: nombre (required: [nombre])
 */
public record ProveedorRequestDTO(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 20)            String nif,
        @Size(max = 100)           String contactoNombre,
        @Size(max = 20)            String telefono,
                                   String email,
        @Size(max = 255)           String direccion,
        @Size(max = 100)           String ciudad,
                                   String notas
) {}