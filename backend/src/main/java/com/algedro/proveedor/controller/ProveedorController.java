package com.algedro.proveedor.controller;

import com.algedro.common.dto.PageResponse;
import com.algedro.proveedor.dto.ProveedorRequestDTO;
import com.algedro.proveedor.dto.ProveedorResponseDTO;
import com.algedro.proveedor.dto.ProveedorResumenDTO;
import com.algedro.proveedor.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ProveedorController — Fase 3
 *
 * Seguridad (openapi.yaml):
 *   GET  /proveedores         → ADMIN + EMPLEADO (lectura)
 *   POST /proveedores         → solo ADMIN
 *   PUT  /proveedores/{id}    → solo ADMIN
 *   DELETE /proveedores/{id}  → solo ADMIN
 *   PATCH /proveedores/{id}/estado → solo ADMIN
 */
@RestController
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/proveedores  → ADMIN + EMPLEADO
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<PageResponse<ProveedorResponseDTO>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo
    ) {
        return ResponseEntity.ok(
                PageResponse.of(proveedorService.listar(pageable, q, activo))
        );
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ProveedorResumenDTO>> resumen() {
        return ResponseEntity.ok(proveedorService.listarResumen());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<ProveedorResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtener(id));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/proveedores  → solo ADMIN → 201 Created
    // ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponseDTO> crear(
            @Valid @RequestBody ProveedorRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequestDTO dto
    ) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean activo = body.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(proveedorService.cambiarEstado(id, activo));
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/v1/proveedores/{id}  → solo ADMIN → soft-delete
    // ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        proveedorService.softDelete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
