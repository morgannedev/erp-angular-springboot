package com.algedro.venta.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDetailResponse {
    private Long id;
    private String numeroVenta;
    private Long empleadoId;
    private Long clienteId;
    private OffsetDateTime fecha;
    private BigDecimal subtotal;
    private BigDecimal descuentoGlobal;
    private BigDecimal total;
    private String metodoPago;
    private String estado;
    private String motivoAnulacion;
    private OffsetDateTime fechaAnulacion;
    private String notas;
    private List<VentaLineResponse> lineas;
    private String urlTicket;
}