package com.algedro.venta.controller;

import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.exception.ForbiddenException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.venta.dto.VentaDetailResponse;
import com.algedro.venta.dto.VentaRequest;
import com.algedro.venta.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final EmpleadoRepository empleadoRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<VentaDetailResponse> crear(@RequestBody VentaRequest request, Authentication authentication) {
        Long empleadoId = getEmpleadoId(authentication);
        String rol = getRol(authentication);
        VentaDetailResponse response = ventaService.crear(request, empleadoId, rol);
        return ResponseEntity
                .created(URI.create("/api/v1/ventas/" + response.getId()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) Long empleadoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "false") boolean exportar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication) {

        String rol = getRol(authentication);
        if (exportar) {
            if (!"ADMIN".equals(rol)) {
                throw new ForbiddenException("Exportacion CSV restringida a ADMIN");
            }
            String csv = ventaService.exportarCsv(desde, hasta);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventas.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        Long empleadoFiltro = "EMPLEADO".equals(rol) ? getEmpleadoId(authentication) : empleadoId;
        Page<VentaDetailResponse> ventas = ventaService.listar(
                desde, hasta, empleadoFiltro, clienteId, metodoPago, estado, PageRequest.of(page, size), rol);
        return ResponseEntity.ok(ventas);
    }

    @PostMapping("/{ventaId}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VentaDetailResponse> anular(
        @PathVariable Long ventaId,
        @RequestBody Map<String, String> body,
        Authentication authentication) throws BadRequestException {
        if (!"ADMIN".equals(getRol(authentication))) {
            throw new ForbiddenException("Solo ADMIN puede anular ventas");
        }
        String motivo = body == null ? null : body.get("motivoAnulacion");
        VentaDetailResponse response = ventaService.anular(ventaId, motivo, getEmpleadoId(authentication));
        return ResponseEntity.ok(response);
    }

    private Long getEmpleadoId(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        if (username == null) {
            throw new ResourceNotFoundException("Empleado autenticado no encontrado");
        }
        return empleadoRepository.findByUsuarioUsername(username)
                .map(e -> e.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado autenticado no encontrado: " + username));
    }

    private String getRol(Authentication authentication) {
        if (authentication != null
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "ADMIN";
        }
        return "EMPLEADO";
    }
}
