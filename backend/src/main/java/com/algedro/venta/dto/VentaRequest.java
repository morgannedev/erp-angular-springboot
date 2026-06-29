package com.algedro.venta.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {
    private List<VentaLineRequest> lineas;
    private Long clienteId;
    private BigDecimal descuentoGlobal;
    private String metodoPago; // EFECTIVO, TARJETA, OTRO
    private String notas;
    private boolean forzarSinStock;
}