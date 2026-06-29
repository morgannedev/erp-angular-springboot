package com.algedro.empleado.mapper;

import com.algedro.empleado.entity.Empleado;
import com.algedro.empleado.dto.EmpleadoResponseDTO;
import com.algedro.empleado.enums.RolEmpleado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EmpleadoMapper {

    public EmpleadoResponseDTO toResponseDTO(Empleado empleado) {
        if (empleado == null) {
            return null;
        }

        EmpleadoResponseDTO dto = new EmpleadoResponseDTO();
        dto.setId(empleado.getId());
        dto.setNombre(empleado.getNombre());
        dto.setApellidos(empleado.getApellidos());
        dto.setDni(empleado.getDni());
        dto.setTelefono(empleado.getTelefono());
        dto.setEmail(empleado.getEmail());
        dto.setCargo(empleado.getCargo());
        dto.setFechaContratacion(empleado.getFechaContratacion());
        dto.setFechaBaja(empleado.getFechaBaja());
        dto.setNotas(empleado.getNotes()); // Sincroniza 'notes' con 'notas'
        
        // Conversión segura de Double a BigDecimal para el salario
        if (empleado.getSalario() != null) {
            dto.setSalario(empleado.getSalario().doubleValue());
        }

        // Extraer datos de la relación @OneToOne con Usuario de forma segura
        if (empleado.getUsuario() != null) {
            dto.setUsuarioId(empleado.getUsuario().getId());
            dto.setUsername(empleado.getUsuario().getUsername());
            if (empleado.getUsuario().getRol() != null) {
                dto.setRol(RolEmpleado.valueOf(empleado.getUsuario().getRol().name()));
            }
            dto.setCuentaActiva(empleado.getUsuario().isActivo());
        }

        // Tiempos de auditoría
        dto.setCreatedAt(empleado.getCreatedAt());
        dto.setUpdatedAt(empleado.getUpdatedAt());

        return dto;
    }
}
