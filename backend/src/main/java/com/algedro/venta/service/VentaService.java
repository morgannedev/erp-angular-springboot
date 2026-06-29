package com.algedro.venta.service;

import com.algedro.cliente.entity.Cliente;
import com.algedro.cliente.repository.ClienteRepository;
import com.algedro.empleado.entity.Empleado;
import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ForbiddenException;
import com.algedro.exception.InsufficientStockException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.stock.entity.MovimientoStock;
import com.algedro.stock.entity.TipoMovimientoStock;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.venta.entity.DetalleVenta;
import com.algedro.venta.entity.Venta;
import com.algedro.venta.dto.*;
import com.algedro.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final EmpleadoRepository empleadoRepository;

    /**
     * REGISTRO DE VENTA (POS)
     * Descuenta stock, genera snapshots e inserta movimientos de inventario de forma atómica.
     */
    @Transactional
    public VentaDetailResponse crear(VentaRequest request, Long empleadoId, String rolUsuario) {
        // 1. Criterio de Aceptación: Una venta no puede finalizarse con cero líneas de producto.
        if (request.getLineas() == null || request.getLineas().isEmpty()) {
            sneakyThrow(new BadRequestException("No se puede procesar una venta con cero líneas de producto."));
        }

        // 2. Criterio de Aceptación: Descuento global y descuentos por línea son mutuamente excluyentes.
        boolean tieneDescuentoGlobal = request.getDescuentoGlobal() != null && request.getDescuentoGlobal().compareTo(BigDecimal.ZERO) > 0;
        boolean tieneDescuentoPorLinea = request.getLineas().stream()
                .anyMatch(l -> l.getDescuentoLinea() != null && l.getDescuentoLinea().compareTo(BigDecimal.ZERO) > 0);

        if (tieneDescuentoGlobal && tieneDescuentoPorLinea) {
            sneakyThrow(new BadRequestException("Regla comercial infringida: Los descuentos por línea y el descuento global son mutuamente excluyentes."));
        }

        // 3. Vincular cliente si se provee en el request
        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente asociado no encontrado con ID: " + request.getClienteId()));
        }

        // 4. Inicializar cabecera temporal para cálculos acumulativos
        String numeroTicket = "VTA-" + OffsetDateTime.now().getYear() + "-" + String.format("%05d", ventaRepository.count() + 1);
        Venta venta = Venta.builder()
                .numeroVenta(numeroTicket)
                .empleadoId(empleadoId)
                .cliente(cliente)
                .fecha(OffsetDateTime.now())
                .metodoPago(request.getMetodoPago())
                .estado("COMPLETADA")
                .descuentoGlobal(request.getDescuentoGlobal() != null ? request.getDescuentoGlobal() : BigDecimal.ZERO)
                .notas(request.getNotas())
                .lineas(new ArrayList<>())
                .build();

        BigDecimal subtotalAcumulado = BigDecimal.ZERO;
        List<MovimientoStock> movimientosPendientes = new ArrayList<>();
        Empleado empleadoActivo = empleadoRepository.findById(empleadoId).orElse(null);

        // 5. Iterar líneas, validar Catálogo y procesar inventarios de forma segura
        for (VentaLineRequest lineReq : request.getLineas()) {
            Producto producto = productoRepository.findById(lineReq.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no registrado con ID: " + lineReq.getProductoId()));

            // Validación de catálogo activo
            if (!producto.getActivo()) {
                throw new BusinessRuleException("El producto '" + producto.getNombre() + "' esta inactivo y no se puede vender.");
            }

            // Validación de existencias de inventario
            if (producto.getStockActual() < lineReq.getCantidad()) {
                if (request.isForzarSinStock()) {
                    if (!"ADMIN".equalsIgnoreCase(rolUsuario)) {
                        throw new ForbiddenException("Permiso denegado: Solo el rol ADMIN puede forzar ventas sin stock en el POS.");
                    }
                } else {
                    throw new InsufficientStockException("Stock insuficiente para el producto: " + producto.getNombre());
                }
            }

            // Capturar Snapshots inmutables de la línea de venta
            BigDecimal precioSnapshot = producto.getPrecioUnitario();
            BigDecimal descuentoLinea = lineReq.getDescuentoLinea() != null ? lineReq.getDescuentoLinea() : BigDecimal.ZERO;

            BigDecimal subtotalLinea = precioSnapshot.multiply(BigDecimal.valueOf(lineReq.getCantidad()))
                    .subtract(descuentoLinea);

            subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);

            DetalleVenta detalle = DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(lineReq.getCantidad())
                    .precioUnitario(precioSnapshot)
                    .nombreProducto(producto.getNombre())
                    .descuentoLinea(descuentoLinea)
                    .subtotalLinea(subtotalLinea)
                    .build();

            venta.addLinea(detalle);

            // Reducción física del stock actual del producto en catálogo
            producto.setStockActual(producto.getStockActual() - lineReq.getCantidad());
            productoRepository.save(producto);

            // ✨ Solución: Instanciación limpia usando el constructor explícito inalterado de MovimientoStock
            MovimientoStock movimiento = new MovimientoStock(
                    producto,
                    TipoMovimientoStock.VENTA,
                    -lineReq.getCantidad(), // Negativo representa salida
                    producto.getStockActual(),
                    "Venta POS registrada en ticket " + numeroTicket,
                    null, // El ventaId temporal se asocia en el paso 8 post-save
                    null,
                    null,
                    empleadoActivo
            );

            movimientosPendientes.add(movimiento);
        }

        // 6. Asignar cálculos financieros definitivos a la cabecera
        venta.setSubtotal(subtotalAcumulado);
        BigDecimal totalFinal = subtotalAcumulado.subtract(venta.getDescuentoGlobal());
        venta.setTotal(totalFinal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalFinal);

        // 7. Persistir la venta total con sus líneas en cascada profunda
        Venta ventaGuardada = ventaRepository.save(venta);

        // 8. Sincronizar movimientos de stock con el ID final de la venta guardada
        for (MovimientoStock ms : movimientosPendientes) {
            ms.asociarVenta(ventaGuardada.getId());
            movimientoStockRepository.save(ms);
        }

        return mapearADetailResponse(ventaGuardada);
    }

    /**
     * ANULACIÓN DE VENTAS (Solo ADMIN)
     * Cambia el estado a ANULADA, reintegra stocks físicos y emite contra-movimientos auditoriales.
     */
    @Transactional
    public VentaDetailResponse anular(Long ventaId, String motivoAnulacion, Long adminId) throws BadRequestException {
        if (motivoAnulacion == null || motivoAnulacion.trim().isEmpty()) {
            throw new BadRequestException("Es obligatorio proporcionar un motivo de texto para proceder con la anulación.");
        }

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + ventaId));

        if ("ANULADA".equalsIgnoreCase(venta.getEstado())) {
            throw new ConflictException("Conflicto: La venta con ID: " + ventaId + " ya se encuentra anulada.");
        }

        // Modificar cabecera conforme las reglas de la restricción CHECK de Postgres
        venta.setEstado("ANULADA");
        venta.setMotivoAnulacion(motivoAnulacion);
        venta.setAnuladaPor(adminId);
        venta.setFechaAnulacion(OffsetDateTime.now());

        // Corregido: Buscar usando adminId que es el parámetro real de la firma ejecutora
        Empleado administradorActivo = empleadoRepository.findById(adminId).orElse(null);

        // Revertir stock de cada una de las líneas originales
        for (DetalleVenta linea : venta.getLineas()) {
            Producto producto = linea.getProducto();
            producto.setStockActual(producto.getStockActual() + linea.getCantidad()); // Reincorporamos unidades
            productoRepository.save(producto);

            // Corregido: Uso del constructor público plano en vez de .builder() inexistente
            MovimientoStock movimientoReversion = new MovimientoStock(
                    producto,
                    TipoMovimientoStock.ANULACION_VENTA,
                    linea.getCantidad(), // Re-entrada positiva
                    producto.getStockActual(),
                    "Anulación de Venta: " + motivoAnulacion,
                    venta.getId(),
                    null,
                    null,
                    administradorActivo
            );

            movimientoStockRepository.save(movimientoReversion);
        }

        Venta ventaAnulada = ventaRepository.save(venta);
        return mapearADetailResponse(ventaAnulada);
    }

    /**
     * HISTORIAL FILTRABLE CON RESOLUCIÓN DINÁMICA DE ROLES
     */
    @Transactional(readOnly = true)
    public Page<VentaDetailResponse> listar(LocalDate desde, LocalDate hasta, Long empleadoId, Long clienteId,
                                            String metodoPago, String estado, Pageable pageable, String rolUsuario) {

        // Convertir LocalDate a OffsetDateTime con el inicio y fin del día
        OffsetDateTime desdeODT = null;
        OffsetDateTime hastaODT = null;

        if (desde != null) {
            desdeODT = desde.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        }

        if (hasta != null) {
            hastaODT = hasta.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
        }

        if ("EMPLEADO".equalsIgnoreCase(rolUsuario)) {
            empleadoId = empleadoId;
        }

        Page<Venta> paginaVentas = ventaRepository.buscarVentasFiltradas(desdeODT, hastaODT, empleadoId, clienteId, metodoPago, estado, pageable);
        return paginaVentas.map(this::mapearADetailResponse);
    }

    @Transactional(readOnly = true)
    public String exportarCsv(LocalDate desde, LocalDate hasta) {
        OffsetDateTime desdeODT = desde != null ? desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()) : null;
        OffsetDateTime hastaODT = hasta != null ? hasta.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset()) : null;

        StringBuilder csv = new StringBuilder("id,numeroVenta,fecha,empleadoId,clienteId,subtotal,descuentoGlobal,total,metodoPago,estado\n");
        for (Venta venta : ventaRepository.findByFechaBetweenParaExportar(desdeODT, hastaODT)) {
            csv.append(venta.getId()).append(',')
                    .append(venta.getNumeroVenta()).append(',')
                    .append(venta.getFecha()).append(',')
                    .append(venta.getEmpleadoId()).append(',')
                    .append(venta.getCliente() != null ? venta.getCliente().getId() : "").append(',')
                    .append(venta.getSubtotal()).append(',')
                    .append(venta.getDescuentoGlobal()).append(',')
                    .append(venta.getTotal()).append(',')
                    .append(venta.getMetodoPago()).append(',')
                    .append(venta.getEstado()).append('\n');
        }
        return csv.toString();
    }

    // ── Métodos Helper de Mapeo Interno (Evitan dependencias circulares) ──

    private VentaDetailResponse mapearADetailResponse(Venta v) {
        List<VentaLineResponse> lineasDto = v.getLineas().stream().map(l -> VentaLineResponse.builder()
                .id(l.getId())
                .productoId(l.getProducto().getId())
                .nombreProducto(l.getNombreProducto()) // Trae el snapshot histórico del texto
                .cantidad(l.getCantidad())
                .precioUnitario(l.getPrecioUnitario()) // Trae el snapshot histórico del precio
                .descuentoLinea(l.getDescuentoLinea())
                .subtotalLinea(l.getSubtotalLinea())
                .build()
        ).collect(Collectors.toList());

        return VentaDetailResponse.builder()
                .id(v.getId())
                .numeroVenta(v.getNumeroVenta())
                .empleadoId(v.getEmpleadoId())
                .clienteId(v.getCliente() != null ? v.getCliente().getId() : null)
                .fecha(v.getFecha())
                .subtotal(v.getSubtotal())
                .descuentoGlobal(v.getDescuentoGlobal())
                .total(v.getTotal())
                .metodoPago(v.getMetodoPago())
                .estado(v.getEstado())
                .motivoAnulacion(v.getMotivoAnulacion())
                .fechaAnulacion(v.getFechaAnulacion())
                .notas(v.getNotas())
                .lineas(lineasDto)
                .urlTicket("/api/ventas/" + v.getId() + "/ticket")
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }
}
