package com.algedro.cliente.mapper;

import com.algedro.cliente.dto.ClienteRequest;
import com.algedro.cliente.dto.ClienteResponse;
import com.algedro.cliente.entity.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    /**
     * Transforma un ClienteRequest (DTO Lombok) a una Entidad Cliente (JPA)
     */
    public Cliente toEntity(ClienteRequest request) {
        if (request == null) {
            return null;
        }

        Cliente cliente = new Cliente();
        // 🔄 CORREGIDO: Uso de getters estándar de Lombok
        cliente.setNombre(request.getNombre());
        cliente.setApellidos(request.getApellidos());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setNif(request.getNif());
        cliente.setDireccion(request.getDireccion());
        cliente.setCiudad(request.getCiudad());
        cliente.setNotas(request.getNotas());
        cliente.setActivo(true);

        return cliente;
    }

    /**
     * Transforma una Entidad Cliente (JPA) a un ClienteResponse (DTO)
     */
    /**
     * Transforma una Entidad Cliente (JPA) a un ClienteResponse (DTO)
     */
    public ClienteResponse toResponse(Cliente entity) {
        if (entity == null) {
            return null;
        }

        return ClienteResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .apellidos(entity.getApellidos())
                .telefono(entity.getTelefono())
                .email(entity.getEmail())
                .nif(entity.getNif())
                .direccion(entity.getDireccion())
                .ciudad(entity.getCiudad())
                .notas(entity.getNotas())
                .activo(entity.isActivo())
                .build();
    }

    /**
     * Actualiza una entidad existente utilizando los datos del Request DTO
     */
    public void updateEntityFromRequest(ClienteRequest request, Cliente entity) {
        if (request == null || entity == null) {
            return;
        }

        // 🔄 CORREGIDO: Uso de getters estándar de Lombok
        entity.setNombre(request.getNombre());
        entity.setApellidos(request.getApellidos());
        entity.setTelefono(request.getTelefono());
        entity.setEmail(request.getEmail());
        entity.setNif(request.getNif());
        entity.setDireccion(request.getDireccion());
        entity.setCiudad(request.getCiudad());
        entity.setNotas(request.getNotas());
    }
}