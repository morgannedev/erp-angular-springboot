package com.algedro.empleado.controller;

import com.algedro.empleado.dto.EmpleadoCreateRequestDTO;
import com.algedro.empleado.dto.EmpleadoResponseDTO;
import com.algedro.empleado.dto.EmpleadoUpdateRequestDTO;
import com.algedro.empleado.service.EmpleadoService;
import com.algedro.usuario.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empleados")
@PreAuthorize("hasRole('ADMIN')")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    /**
     * GET /empleados : Listar empleados de manera paginada y con filtros opcionales.
     */
    @GetMapping
    public ResponseEntity<Page<EmpleadoResponseDTO>> listarEmpleados(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "apellidos") Pageable pageable) {
        
        Page<EmpleadoResponseDTO> empleados = empleadoService.buscarConFiltros(q, activo, pageable);
        return ResponseEntity.ok(empleados);
    }

    /**
     * POST /empleados : Crear un nuevo empleado junto con su cuenta de usuario asociada.
     */
    @PostMapping
    public ResponseEntity<EmpleadoResponseDTO> crearEmpleado(
            @Valid @RequestBody EmpleadoCreateRequestDTO request) {

        // CORREGIDO: Se cambia 'crearEmpleadoConUsuario' por 'crearEmpleado'
        EmpleadoResponseDTO nuevoEmpleado = empleadoService.crearEmpleado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEmpleado);
    }

    /**
     * GET /empleados/{empleadoId} : Obtener detalles de un empleado por su ID.
     */
    @GetMapping("/{empleadoId}")
    public ResponseEntity<EmpleadoResponseDTO> obtenerEmpleadoPorId(
            @PathVariable("empleadoId") Long empleadoId) {

        // CORREGIDO: Se cambia 'obtenerPorId' por 'getEmpleadoPorId'
        EmpleadoResponseDTO empleado = empleadoService.getEmpleadoPorId(empleadoId);
        return ResponseEntity.ok(empleado);
    }

    /**
     * PUT /empleados/{empleadoId} : Actualizar los datos de un empleado existente.
     */
    @PutMapping("/{empleadoId}")
    public ResponseEntity<EmpleadoResponseDTO> actualizarEmpleado(
            @PathVariable("empleadoId") Long empleadoId,
            @Valid @RequestBody EmpleadoUpdateRequestDTO request) {
        
        EmpleadoResponseDTO empleadoActualizado = empleadoService.actualizarEmpleado(empleadoId, request);
        return ResponseEntity.ok(empleadoActualizado);
    }

    /**
     * PATCH /empleados/{empleadoId}/cuenta : Activar o desactivar la cuenta del usuario relacionado.
     */
    @PatchMapping("/{empleadoId}/cuenta")
    public ResponseEntity<EmpleadoResponseDTO> cambiarEstadoCuenta(
            @PathVariable("empleadoId") Long empleadoId,
            @Valid @RequestBody ActivarCuentaRequest request) {

        EmpleadoResponseDTO empleadoModificado = empleadoService.cambiarEstadoCuenta(empleadoId, request.activo());
        return ResponseEntity.ok(empleadoModificado);
    }

    /**
     * PATCH /empleados/{empleadoId}/rol : Cambiar el rol asignado al usuario relacionado.
     */
    @PatchMapping("/{empleadoId}/rol")
    public ResponseEntity<EmpleadoResponseDTO> cambiarRolCuenta(
            @PathVariable("empleadoId") Long empleadoId,
            @Valid @RequestBody CambiarRolRequest request) {

        EmpleadoResponseDTO empleadoModificado = empleadoService.cambiarRol(empleadoId, request.rol().name());
        return ResponseEntity.ok(empleadoModificado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarEmpleado(@PathVariable Long id) {
        empleadoService.eliminarEmpleado(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs Inline específicos para los payloads parciales (PATCH) utilizando Records de Java 17
    // ─────────────────────────────────────────────────────────────────────────

    public record ActivarCuentaRequest(
            @NotNull(message = "El estado 'activo' es obligatorio") Boolean activo
    ) {}

    public record CambiarRolRequest(
            @NotNull(message = "El 'rol' es obligatorio") Rol rol
    ) {}
}
