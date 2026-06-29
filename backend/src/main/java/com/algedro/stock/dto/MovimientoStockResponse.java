package com.algedro.stock.dto;

import java.time.OffsetDateTime;

public record MovimientoStockResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String tipo,
        Integer cantidad,
        Integer stockResultante,
        String motivo,
        Long ventaId,
        Long proveedorId,
        String albaran,
        Long empleadoId,
        String empleadoNombre,
        OffsetDateTime fecha
) {}
