package com.algedro.stock.dto;

public record StockNivelResponse(
        Long productoId,
        String referencia,
        String nombre,
        Integer stockActual,
        Integer stockMinimo,
        Integer stockMaximo,
        Boolean enAlerta,
        Long proveedorId,
        String proveedorNombre
) {}
