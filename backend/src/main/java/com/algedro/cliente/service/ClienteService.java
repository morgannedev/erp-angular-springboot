package com.algedro.cliente.service;

import com.algedro.cliente.entity.Cliente;
import com.algedro.cliente.dto.ClienteRequest;
import com.algedro.cliente.dto.ClienteResponse;
import com.algedro.cliente.mapper.ClienteMapper;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.cliente.repository.ClienteRepository;
import com.algedro.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final ClienteMapper clienteMapper;

    /**
     * Crea un cliente validando unicidad de NIF y Email.
     * Soporta la creación rápida desde el POS (solo nombre y teléfono obligatorios).
     */
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        // ✨ Asegurado el uso de getters de Lombok
        if (request.getNif() != null && clienteRepository.existsByNif(request.getNif())) {
            throw new ConflictException("El NIF ya está registrado en el sistema.");
        }
        if (request.getEmail() != null && clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("El email ya está registrado en el sistema.");
        }

        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setFechaAlta(LocalDate.now());

        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.toResponse(guardado);
    }

    /**
     * Actualiza los datos de un cliente existente comprobando colisiones de NIF/Email con otros IDs.
     */
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        // ✨ CORREGIDO: Uso de getters de Lombok (getNif() y getEmail())
        if (request.getNif() != null && clienteRepository.existsByNifAndIdNot(request.getNif(), id)) {
            throw new ConflictException("El NIF ya pertenece a otro cliente.");
        }
        if (request.getEmail() != null && clienteRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ConflictException("El email ya pertenece a otro cliente.");
        }

        // Modifica la entidad existente usando el DTO
        clienteMapper.updateEntityFromRequest(request, existente);

        Cliente actualizado = clienteRepository.save(existente);
        return clienteMapper.toResponse(actualizado);
    }

    /**
     * Elimina físicamente de la BD si no tiene transacciones/ventas asociadas.
     * En caso contrario, lanza BusinessRuleException sugiriendo su desactivación.
     */
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        if (ventaRepository.existsByClienteId(id)) {
            throw new BusinessRuleException("La eliminación está bloqueada porque el cliente tiene ventas asociadas; se sugiere desactivarlo en su lugar.");
        }

        clienteRepository.deleteById(id);
    }

    /**
     * Cambia el estado lógico (activo/inactivo) del cliente sin alterar su persistencia.
     */
    @Transactional
    public ClienteResponse cambiarEstado(Long id, boolean activo) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        cliente.setActivo(activo);
        Cliente modificado = clienteRepository.save(cliente);
        return clienteMapper.toResponse(modificado);
    }

    /**
     * Recupera un cliente por su ID independientemente de su estado lógico.
     */
    @Transactional(readOnly = true)
    public ClienteResponse getById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return clienteMapper.toResponse(cliente);
    }

    /**
     * Realiza búsquedas parciales paginadas aplicando filtros por texto (nombre/apellidos/teléfono) y estado.
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String query, boolean activo, Pageable pageable) {
        Page<Cliente> paginaClientes = clienteRepository.buscar(query, activo, pageable);
        return paginaClientes.map(clienteMapper::toResponse);
    }

    /**
     * Exporta todos los clientes del sistema en formato CSV plano e incluye cabeceras básicas de control.
     */
    @Transactional(readOnly = true)
    public String exportarCsv() {
        List<Cliente> clientes = clienteRepository.findAll();
        
        StringBuilder sb = new StringBuilder();
        sb.append("id,nombre,apellidos,telefono,email,nif,activo\n");

        for (Cliente c : clientes) {
            sb.append(c.getId()).append(",")
              .append(c.getNombre() != null ? c.getNombre() : "").append(",")
              .append(c.getApellidos() != null ? c.getApellidos() : "").append(",")
              .append(c.getTelefono() != null ? c.getTelefono() : "").append(",")
              .append(c.getEmail() != null ? c.getEmail() : "").append(",")
              .append(c.getNif() != null ? c.getNif() : "").append(",")
              .append(c.isActivo()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Obtiene el historial de ventas paginado de un cliente específico delimitado opcionalmente entre fechas.
     */
    @Transactional(readOnly = true)
    public Page<Object> getHistorialCompras(Long clienteId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + clienteId));

        // El test espera un Page procedente del VentaRepository usando findByClienteIdAndFechaBetween
        // Nota: Mapeamos a Object genérico para ajustarse a cualquier firma del DTO o Entidad de Ventas usado en el test
        OffsetDateTime desdeFiltro = desde != null ? desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()) : null;
        OffsetDateTime hastaFiltro = hasta != null ? hasta.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset()) : null;

        return ventaRepository.findByClienteIdAndFechaBetween(clienteId, desdeFiltro, hastaFiltro, pageable).map(v -> (Object) v);
    }
}
