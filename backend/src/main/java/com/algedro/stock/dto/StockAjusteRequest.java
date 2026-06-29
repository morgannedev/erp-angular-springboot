package com.algedro.stock.dto;

public record StockAjusteRequest(
        Integer cantidad,
        String motivo,
        Boolean forzarNegativo
) {}
