package com.algedro.proveedor.service;


import com.algedro.exception.ConflictoException;
import com.algedro.exception.RecursoNoEncontradoException;
import com.algedro.proveedor.dto.ProveedorRequestDTO;
import com.algedro.proveedor.dto.ProveedorResponseDTO;
import com.algedro.proveedor.dto.ProveedorResumenDTO;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.proveedor.repository.ProveedorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProveedorService — implementación GREEN para la Fase 3.
 *
 * Reglas de negocio implementadas:
 *  1. crear()     → NIF único (ConflictoException si ya existe)
 *  2. softDelete() → activo=false, nunca deleteById (openapi.yaml §DELETE /proveedores)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    // ─────────────────────────────────────────────────────────────
    // crear
    // ─────────────────────────────────────────────────────────────

    /**
     * Crea un nuevo proveedor.
     * @throws ConflictoException HTTP 409 si el NIF ya está registrado.
     */
    public ProveedorResponseDTO crear(ProveedorRequestDTO dto) {
        // Regla: NIF único (Data-Model.md §1.3: nif UNIQUE NULL)
        if (dto.nif() != null) {
            proveedorRepository.findByNif(dto.nif()).ifPresent(existente -> {
                throw new ConflictoException(
                        "El NIF " + dto.nif() + " ya está registrado en otro proveedor (id=" + existente.getId() + ")."
                );
            });
        }

        Proveedor proveedor = toEntity(dto);
        Proveedor guardado = proveedorRepository.save(proveedor);
        return toResponse(guardado);
    }

    // ─────────────────────────────────────────────────────────────
    // softDelete
    // ─────────────────────────────────────────────────────────────

    /**
     * Desactiva el proveedor (activo = false). No lo elimina físicamente.
     * openapi.yaml: "Se recomienda desactivar en lugar de eliminar.
     *               Los productos vinculados quedan con proveedor_id = NULL (ON DELETE SET NULL)."
     *
     * @throws RecursoNoEncontradoException HTTP 404 si no existe.
     */
    public void softDelete(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado con id=" + id
                ));

        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
        // NUNCA se llama a deleteById() — el registro se conserva
        // para mantener la integridad del historial de productos
    }

    @Transactional(readOnly = true)
    public ProveedorResponseDTO obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    public ProveedorResponseDTO actualizar(Long id, ProveedorRequestDTO dto) {
        Proveedor proveedor = buscarPorId(id);
        if (dto.nif() != null) {
            proveedorRepository.findByNif(dto.nif())
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> {
                        throw new ConflictoException("El NIF " + dto.nif() + " ya esta registrado en otro proveedor.");
                    });
        }

        proveedor.setNombre(dto.nombre());
        proveedor.setNif(dto.nif());
        proveedor.setContactoNombre(dto.contactoNombre());
        proveedor.setTelefono(dto.telefono());
        proveedor.setEmail(dto.email());
        proveedor.setDireccion(dto.direccion());
        proveedor.setCiudad(dto.ciudad());
        proveedor.setNotas(dto.notas());
        return toResponse(proveedorRepository.save(proveedor));
    }

    public ProveedorResponseDTO cambiarEstado(Long id, boolean activo) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setActivo(activo);
        return toResponse(proveedorRepository.save(proveedor));
    }

    // ─────────────────────────────────────────────────────────────
    // listar
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> listar(Pageable pageable, String q, Boolean activo) {
        return proveedorRepository.buscar(q, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProveedorResumenDTO> listarResumen() {
        return proveedorRepository.buscar(null, true, Pageable.unpaged()).stream()
                .map(p -> new ProveedorResumenDTO(p.getId(), p.getNombre()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // Mappers privados
    // ─────────────────────────────────────────────────────────────

    private Proveedor toEntity(ProveedorRequestDTO dto) {
        Proveedor p = new Proveedor();
        p.setNombre(dto.nombre());
        p.setNif(dto.nif());
        p.setContactoNombre(dto.contactoNombre());
        p.setTelefono(dto.telefono());
        p.setEmail(dto.email());
        p.setDireccion(dto.direccion());
        p.setCiudad(dto.ciudad());
        p.setNotas(dto.notas());
        p.setActivo(true);
        return p;
    }

    private Proveedor buscarPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado con id=" + id));
    }

    private ProveedorResponseDTO toResponse(Proveedor p) {
        return new ProveedorResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getNif(),
                p.getContactoNombre(),
                p.getTelefono(),
                p.getEmail(),
                p.getDireccion(),
                p.getCiudad(),
                p.isActivo(),
                p.getNotas(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
