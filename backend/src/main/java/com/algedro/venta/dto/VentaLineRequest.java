package com.algedro.venta.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaLineRequest {
    private Long productoId;
    private Integer cantidad;
    private BigDecimal descuentoLinea;
}