package com.algedro.stock.dto;

import jakarta.validation.constraints.Min;

public record StockEntradaRequest(
        @Min(1) Integer cantidad,
        Long proveedorId,
        String albaran
) {}
