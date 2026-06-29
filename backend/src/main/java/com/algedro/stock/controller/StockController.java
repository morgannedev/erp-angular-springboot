package com.algedro.stock.controller;

import com.algedro.common.dto.PageResponse;
import com.algedro.stock.dto.MovimientoStockResponse;
import com.algedro.stock.dto.StockAjusteRequest;
import com.algedro.stock.dto.StockEntradaRequest;
import com.algedro.stock.dto.StockNivelResponse;
import com.algedro.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // ✅ EMPLEADO puede ver stock
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<PageResponse<StockNivelResponse>> listarStock(
            @RequestParam(defaultValue = "false") Boolean soloAlertas,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String query,  // ← NUEVO: búsqueda textual
            Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.of(
                stockService.listarStock(soloAlertas, productoId, proveedorId, query, pageable)
        ));
    }

    // ✅ EMPLEADO puede ver historial
    @GetMapping("/{productoId}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<PageResponse<MovimientoStockResponse>> historial(
            @PathVariable Long productoId,
            @RequestParam(required = false) String tipo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(stockService.historial(productoId, tipo, pageable));
    }

    // Solo ADMIN puede modificar
    @PostMapping("/{productoId}/entradas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoStockResponse> registrarEntrada(
            @PathVariable Long productoId,
            @Valid @RequestBody StockEntradaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registrarEntrada(productoId, request));
    }

    @PostMapping("/{productoId}/ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoStockResponse> registrarAjuste(
            @PathVariable Long productoId,
            @Valid @RequestBody StockAjusteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registrarAjuste(productoId, request));
    }

    // NUEVO: Endpoints para editar mínimo/máximo
    @PatchMapping("/{productoId}/minimo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> actualizarMinimo(
            @PathVariable Long productoId,
            @RequestBody Integer nuevoMinimo
    ) {
        stockService.actualizarStockMinimo(productoId, nuevoMinimo);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{productoId}/maximo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> actualizarMaximo(
            @PathVariable Long productoId,
            @RequestBody Integer nuevoMaximo
    ) {
        stockService.actualizarStockMaximo(productoId, nuevoMaximo);
        return ResponseEntity.ok().build();
    }
}
