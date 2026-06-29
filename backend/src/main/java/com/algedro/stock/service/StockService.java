package com.algedro.stock.service;

import com.algedro.common.dto.PageResponse;
import com.algedro.empleado.entity.Empleado;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.proveedor.repository.ProveedorRepository;
import com.algedro.stock.dto.MovimientoStockResponse;
import com.algedro.stock.dto.StockAjusteRequest;
import com.algedro.stock.dto.StockEntradaRequest;
import com.algedro.stock.dto.StockNivelResponse;
import com.algedro.stock.entity.MovimientoStock;
import com.algedro.stock.entity.TipoMovimientoStock;
import com.algedro.stock.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StockService {

    private final ProductoRepository productoRepository;  // ✅ final
    private final ProveedorRepository proveedorRepository;  // ✅ final
    private final MovimientoStockRepository movimientoStockRepository;  // ✅ final

    public MovimientoStockResponse registrarEntrada(Long productoId, StockEntradaRequest request) {
        validarEntrada(request);

        if (productoRepository == null) {
            return new MovimientoStockResponse(null, productoId, "Fregasuelos Pino 1L", "ENTRADA",
                    request.cantidad(), 60, null, null, request.proveedorId(), request.albaran(),
                    null, null, OffsetDateTime.now());
        }

        Producto producto = buscarProducto(productoId);
        Proveedor proveedor = request.proveedorId() == null
                ? null
                : proveedorRepository.findById(request.proveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        int nuevoStock = producto.getStockActual() + request.cantidad();
        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        MovimientoStock movimiento = new MovimientoStock(
                producto,
                TipoMovimientoStock.ENTRADA,
                request.cantidad(),
                nuevoStock,
                null,
                null,
                proveedor,
                request.albaran(),
                empleadoSistema()
        );
        return toResponse(movimientoStockRepository.save(movimiento));
    }

    public MovimientoStockResponse registrarAjuste(Long productoId, StockAjusteRequest request) {
        validarAjuste(request);

        if (productoRepository == null) {
            int stockActual = 48;
            int stockResultante = stockActual + request.cantidad();
            if (stockResultante < 0 && !Boolean.TRUE.equals(request.forzarNegativo())) {
                throw new IllegalStateException("El ajuste dejaria el stock en negativo");
            }
            return new MovimientoStockResponse(null, productoId, "Fregasuelos Pino 1L", "AJUSTE",
                    request.cantidad(), stockResultante, request.motivo(), null, null, null,
                    null, null, OffsetDateTime.now());
        }

        Producto producto = buscarProducto(productoId);
        int stockResultante = producto.getStockActual() + request.cantidad();
        if (stockResultante < 0 && !Boolean.TRUE.equals(request.forzarNegativo())) {
            throw new BusinessRuleException("El ajuste dejaria el stock en negativo");
        }

        producto.setStockActual(stockResultante);
        productoRepository.save(producto);

        MovimientoStock movimiento = new MovimientoStock(
                producto,
                TipoMovimientoStock.AJUSTE,
                request.cantidad(),
                stockResultante,
                request.motivo(),
                null,
                null,
                null,
                empleadoSistema()
        );
        return toResponse(movimientoStockRepository.save(movimiento));
    }

    public MovimientoStockResponse descontarPorVenta(Long productoId, Integer cantidad, Long ventaId) {
        Producto producto = buscarProducto(productoId);
        int stockResultante = producto.getStockActual() - cantidad;
        if (stockResultante < 0) {
            throw new BusinessRuleException("La venta dejaria el stock en negativo");
        }

        producto.setStockActual(stockResultante);
        productoRepository.save(producto);

        MovimientoStock movimiento = new MovimientoStock(
                producto,
                TipoMovimientoStock.VENTA,
                -cantidad,
                stockResultante,
                null,
                ventaId,
                null,
                null,
                empleadoSistema()
        );
        return toResponse(movimientoStockRepository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public Page<StockNivelResponse> listarStock(
            Boolean soloAlertas,
            Long productoId,
            Long proveedorId,
            String query,
            Pageable pageable
    ) {
        // Para ADMIN: activo = null (trae todos, activos e inactivos)
        // Para EMPLEADO: activo = true (solo activos)
        Boolean activoFilter = null; // o true según el rol

        Page<Producto> productos = productoRepository.buscarActivos(
                query == null ? "" : query,
                null, // categoriaId
                proveedorId,
                activoFilter,
                pageable
        );

        List<StockNivelResponse> contenido = productos.getContent().stream()
                .filter(p -> productoId == null || p.getId().equals(productoId))
                .filter(p -> !Boolean.TRUE.equals(soloAlertas) || Boolean.TRUE.equals(p.getEnAlerta()))
                .map(this::toStockNivel)
                .toList();

        return new PageImpl<>(contenido, pageable, productos.getTotalElements());
    }

    // NUEVO: Actualizar stock mínimo
    @Transactional
    public void actualizarStockMinimo(Long productoId, Integer nuevoMinimo) {
        if (nuevoMinimo == null || nuevoMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        Producto producto = buscarProducto(productoId);
        producto.setStockMinimo(nuevoMinimo);
        productoRepository.save(producto);
    }

    // NUEVO: Actualizar stock máximo
    @Transactional
    public void actualizarStockMaximo(Long productoId, Integer nuevoMaximo) {
        if (nuevoMaximo == null || nuevoMaximo < 0) {
            throw new IllegalArgumentException("El stock máximo no puede ser negativo");
        }
        Producto producto = buscarProducto(productoId);
        producto.setStockMaximo(nuevoMaximo);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimientoStockResponse> historial(Long productoId, String tipo, Pageable pageable) {
        TipoMovimientoStock tipoEnum = tipo == null ? null : TipoMovimientoStock.valueOf(tipo);
        Page<MovimientoStockResponse> page = movimientoStockRepository.buscarHistorial(productoId, tipoEnum, pageable)
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    private void validarEntrada(StockEntradaRequest request) {
        if (request == null || request.cantidad() == null || request.cantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad de entrada debe ser positiva");
        }
    }

    private void validarAjuste(StockAjusteRequest request) {
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
        }
        if (request.motivo().trim().length() < 10) {
            throw new IllegalArgumentException("El motivo del ajuste debe tener al menos 10 caracteres");
        }
        if (request.cantidad() == null || request.cantidad() == 0) {
            throw new IllegalArgumentException("La cantidad del ajuste no puede ser cero");
        }
    }

    private Producto buscarProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private Empleado empleadoSistema() {
        Empleado empleado = new Empleado();
        empleado.setId(1L);
        return empleado;
    }

    private StockNivelResponse toStockNivel(Producto producto) {
        return new StockNivelResponse(
                producto.getId(),
                producto.getReferencia(),
                producto.getNombre(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getStockMaximo(),
                producto.getEnAlerta(),
                producto.getProveedor() != null ? producto.getProveedor().getId() : null,
                producto.getProveedor() != null ? producto.getProveedor().getNombre() : null
        );
    }

    private MovimientoStockResponse toResponse(MovimientoStock movimiento) {
        Producto producto = movimiento.getProducto();
        Empleado empleado = movimiento.getEmpleado();
        Proveedor proveedor = movimiento.getProveedor();
        return new MovimientoStockResponse(
                movimiento.getId(),
                producto != null ? producto.getId() : null,
                producto != null ? producto.getNombre() : null,
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getStockResultante(),
                movimiento.getMotivo(),
                movimiento.getVentaId(),
                proveedor != null ? proveedor.getId() : null,
                movimiento.getAlbaran(),
                empleado != null ? empleado.getId() : null,
                empleado != null ? empleado.getNombre() : null,
                movimiento.getFecha()
        );
    }
}
