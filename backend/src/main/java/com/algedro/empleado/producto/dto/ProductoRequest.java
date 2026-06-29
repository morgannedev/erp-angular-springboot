package com.algedro.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {
    @NotBlank(message = "La referencia es obligatoria")
    private String referencia;

    private String ean;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    private Long proveedorId;

    @NotNull(message = "El precio de venta es obligatorio")
    private BigDecimal precioVenta;

    private BigDecimal precioCoste;
    private Integer stockMinimo;
    private Integer stockMaximo;
    private String unidadMedida;
}