package com.algedro.cliente.controller;

import com.algedro.cliente.dto.ClienteRequest;
import com.algedro.cliente.dto.ClienteResponse;
import com.algedro.cliente.service.ClienteService;
import com.algedro.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * POST /api/v1/clientes/pos
     * CREACIÓN RÁPIDA: Exigencia estricta en controlador para el flujo de caja.
     */
    @PostMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> crearRapidoDesdePos(@RequestBody ClienteRequest request) {
        if (request == null || request.getNombre() == null || request.getTelefono() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre y el teléfono son obligatorios para el registro en el POS."));
        }

        ClienteResponse creado = clienteService.crear(request);
        creado.setNif(null);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/clientes/" + creado.getId()))
                .body(creado);
    }

    /**
     * POST /api/v1/clientes
     * CRUD COMPLETO: Permitimos que pase al Service para que validaciones complejas o excepciones (409) actúen.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crear(@RequestBody ClienteRequest request) {
        if (request == null || request.getNombre() == null) {
            return ResponseEntity.badRequest().build();
        }

        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/clientes/" + creado.getId()))
                .body(creado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<PageResponse<ClienteResponse>> listar(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "activo", defaultValue = "true") boolean activo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ClienteResponse> paginaClientes = clienteService.listar(query, activo, pageable);
        boolean isAdmin = isUserAdmin();

        Page<ClienteResponse> paginaProcesada = paginaClientes.map(cliente -> {
            if (!isAdmin) {
                cliente.setNif(null);
            }
            return cliente;
        });

        return ResponseEntity.ok(PageResponse.of(paginaProcesada));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<ClienteResponse> getById(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.getById(id);
        if (!isUserAdmin()) {
            cliente.setNif(null);
        }
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @RequestBody ClienteRequest request) {
        if (request == null || request.getNombre() == null) {
            return ResponseEntity.badRequest().build();
        }
        ClienteResponse actualizado = clienteService.actualizar(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null || !body.containsKey("activo") || body.get("activo") == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean activo = (boolean) body.get("activo");
        ClienteResponse modificado = clienteService.cambiarEstado(id, activo);
        return ResponseEntity.ok(modificado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
