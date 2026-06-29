package com.algedro.producto.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    private Long id;
    private String referencia;
    private String ean;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private Long proveedorId;
    private BigDecimal precioVenta;
    private BigDecimal precioCoste; // Se volverá null si no es ADMIN
    private String unidadMedida;
    private Integer stockActual;
    private Integer stockMinimo;
    private Integer stockMaximo;
    private boolean enAlerta;
    private boolean activo;
}