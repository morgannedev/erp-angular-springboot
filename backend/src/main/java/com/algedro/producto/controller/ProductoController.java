package com.algedro.producto.controller;

import com.algedro.common.dto.PageResponse;
import com.algedro.producto.dto.ProductoRequest;
import com.algedro.producto.dto.ProductoResponse;
import com.algedro.producto.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos/barcode/{ean} -> POS Endpoint
    // ══════════════════════════════════════════════════════════
    @GetMapping("/barcode/{ean}")
    public ResponseEntity<ProductoResponse> buscarPorBarcode(@PathVariable String ean, Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        // Nota: El servicio maneja internamente la visibilidad del coste según el rol.
        ProductoResponse response = productoService.buscarPorEan(ean);

        // Ajustamos la visibilidad del precioCoste en base al contexto de seguridad actual
        if (!isAdmin) {
            response.setPrecioCoste(null);
        }
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════
    // POST /productos -> Crear
    // ══════════════════════════════════════════════════════════
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request, Authentication authentication) {
        if (!checkIsAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ProductoResponse response = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos -> Listado Paginado y Filtrado
    // ══════════════════════════════════════════════════════════
    @GetMapping
    public ResponseEntity<PageResponse<ProductoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "true") Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication) {

        boolean isAdmin = checkIsAdmin(authentication);
        Pageable pageable = PageRequest.of(page, size);

        var pagina = productoService.listar(categoriaId, proveedorId, q, activo, pageable, isAdmin);
        return ResponseEntity.ok(PageResponse.of(pagina));
    }

    // ══════════════════════════════════════════════════════════
    // GET /productos/{id} -> Detalle por ID
    // ══════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        ProductoResponse response = productoService.getById(id, isAdmin);
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════
    // PUT /productos/{id} -> Actualizar producto completo
    // ══════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request,
            Authentication authentication) {

        if (!checkIsAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductoResponse response = productoService.actualizar(id, request);
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /productos/{id} -> Eliminar
    // ══════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        if (!checkIsAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /productos/{id}/estado -> Desactivar/Activar
    // ══════════════════════════════════════════════════════════
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {

        if (!checkIsAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Boolean activo = body.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().build();
        }
        ProductoResponse response = productoService.cambiarEstado(id, activo);
        return ResponseEntity.ok(response);
    }

    // Helper metodológico para evaluar roles en base a los GrantedAuthorities de Spring Security
    private boolean checkIsAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
