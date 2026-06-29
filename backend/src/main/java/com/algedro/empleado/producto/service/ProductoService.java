package com.algedro.producto.service;

import com.algedro.exception.ConflictException;
import com.algedro.categoria.entity.Categoria;
import com.algedro.categoria.repository.CategoriaRepository;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.producto.dto.ProductoRequest;
import com.algedro.producto.dto.ProductoResponse;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.proveedor.entity.Proveedor;
import com.algedro.proveedor.repository.ProveedorRepository;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.venta.repository.DetalleVentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           ProveedorRepository proveedorRepository,
                           MovimientoStockRepository movimientoStockRepository,
                           DetalleVentaRepository detalleVentaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    // ══════════════════════════════════════════════════════════
    // CREAR PRODUCTO
    // ══════════════════════════════════════════════════════════
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        // 1. Validar existencia de Categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("La categoría indicada no existe"));

        // 2. Validar unicidad del EAN (si se provee)
        if (request.getEan() != null && !request.getEan().isBlank()) {
            if (productoRepository.existsByEan(request.getEan())) {
                throw new ConflictException("El código EAN '" + request.getEan() + "' ya está registrado por otro producto");
            }
        }

        // 3. Validar unicidad de la Referencia / SKU
        if (productoRepository.existsByReferencia(request.getReferencia())) {
            throw new ConflictException("La referencia '" + request.getReferencia() + "' ya está en uso");
        }

        // 4. Buscar Proveedor si viene en el request
        Proveedor proveedor = null;
        if (request.getProveedorId() != null) {
            proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("El proveedor indicado no existe"));
        }

        // 5. Mapear y construir la Entidad
        Producto nuevoProducto = Producto.builder()
                .referencia(request.getReferencia())
                .ean(request.getEan())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(categoria)
                .proveedor(proveedor)
                .precioVenta(request.getPrecioVenta())
                .precioCoste(request.getPrecioCoste())
                .stockActual(0) // Inicialización de negocio
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0)
                .stockMaximo(request.getStockMaximo())
                .unidadMedida(request.getUnidadMedida() != null ? request.getUnidadMedida() : "ud")
                .activo(true)
                .build();

        // 6. Guardar en base de datos
        Producto guardado = productoRepository.save(nuevoProducto);

        // Al crear se asume rol Admin internamente para devolver la respuesta limpia completa
        return convertoToResponse(guardado, true);
    }

    // ══════════════════════════════════════════════════════════
    // ELIMINAR PRODUCTO
    // ══════════════════════════════════════════════════════════
    @Transactional
    public void eliminar(Long id) {
        // 1. Verificar si el producto existe
        productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el ID: " + id));

        // 2. Regla de negocio: Verificar movimientos de stock históricos
        if (movimientoStockRepository.existsByProductoId(id)) {
            throw new BusinessRuleException("No se puede eliminar el producto porque tiene movimientos de stock registrados");
        }

        // 3. Regla de negocio: Verificar líneas de venta históricas
        if (detalleVentaRepository.existsByProductoId(id)) {
            throw new BusinessRuleException("No se puede eliminar el producto porque está asociado a líneas de venta");
        }

        // 4. Proceder con el borrado físico si el catálogo está limpio de este producto
        productoRepository.deleteById(id);
    }

    // ══════════════════════════════════════════════════════════
    // DESACTIVAR / CAMBIAR ESTADO
    // ══════════════════════════════════════════════════════════
    @Transactional
    public ProductoResponse cambiarEstado(Long id, boolean nuevoEstado) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        producto.setActivo(nuevoEstado);
        Producto actualizado = productoRepository.save(producto);
        
        return convertoToResponse(actualizado, true);
    }

    // ══════════════════════════════════════════════════════════
    // BÚSQUEDA POR EAN (PAGO RÁPIDO POS)
    // ══════════════════════════════════════════════════════════
    public ProductoResponse buscarPorEan(String ean) {
        // El test requiere explícitamente buscarPorEanAndActivoTrue para el POS
        Producto producto = productoRepository.findByEanAndActivoTrue(ean)
                .orElseThrow(() -> new ResourceNotFoundException("Producto activo no encontrado para el EAN: " + ean));
        
        return convertoToResponse(producto, false); // Por defecto el POS asume rol empleado (sin coste)
    }

    // ══════════════════════════════════════════════════════════
    // CONSULTA POR ID Y TRATAMIENTO DE ROL
    // ══════════════════════════════════════════════════════════
    public ProductoResponse getById(Long id, boolean isAdmin) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        return convertoToResponse(producto, isAdmin);
    }

    // ══════════════════════════════════════════════════════════
    // PAGINACIÓN Y LISTADO CON FILTROS DINÁMICOS
    // ══════════════════════════════════════════════════════════
    public Page<ProductoResponse> listar(Long categoriaId, Long proveedorId, String query, Boolean activo, Pageable pageable, boolean isAdmin) {
        // Si es ADMIN, ignorar el filtro de activo para mostrar todos los productos
        Boolean filtroActivo = isAdmin ? null : activo;

        // Invoca el método dinámico personalizado del ProductoRepository
        Page<Producto> paginaProductos = productoRepository.buscarActivos(
                categoriaId,
                proveedorId,
                query == null ? "" : query,
                filtroActivo,  // ADMIN: null (todos), EMPLEADO: solo activos
                pageable
        );

        // Mapea la página controlando la restricción del precio de coste según rol
        return paginaProductos.map(producto -> convertoToResponse(producto, isAdmin));
    }

    // ══════════════════════════════════════════════════════════
    // ACTUALIZAR PRODUCTO
    // ══════════════════════════════════════════════════════════
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        // 1. Buscar el producto existente
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        // 2. Validar existencia de Categoría (si cambia)
        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría indicada no existe"));
            producto.setCategoria(categoria);
        }

        // 3. Validar unicidad del EAN (si cambia y no es nulo)
        if (request.getEan() != null && !request.getEan().isBlank()) {
            // Verificar si el EAN ya existe en otro producto
            productoRepository.findByEan(request.getEan())
                    .ifPresent(p -> {
                        if (!p.getId().equals(id)) {
                            throw new ConflictException("El código EAN '" + request.getEan() + "' ya está registrado por otro producto");
                        }
                    });
            producto.setEan(request.getEan());
        }

        // 4. Validar unicidad de la Referencia / SKU (si cambia)
        if (request.getReferencia() != null && !request.getReferencia().isBlank()) {
            productoRepository.findByReferencia(request.getReferencia())
                    .ifPresent(p -> {
                        if (!p.getId().equals(id)) {
                            throw new ConflictException("La referencia '" + request.getReferencia() + "' ya está en uso");
                        }
                    });
            producto.setReferencia(request.getReferencia());
        }

        // 5. Buscar Proveedor (si cambia)
        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("El proveedor indicado no existe"));
            producto.setProveedor(proveedor);
        } else {
            producto.setProveedor(null);
        }

        // 6. Actualizar campos simples
        if (request.getNombre() != null) producto.setNombre(request.getNombre());
        if (request.getDescripcion() != null) producto.setDescripcion(request.getDescripcion());
        if (request.getPrecioVenta() != null) producto.setPrecioVenta(request.getPrecioVenta());
        if (request.getPrecioCoste() != null) producto.setPrecioCoste(request.getPrecioCoste());
        if (request.getStockMinimo() != null) producto.setStockMinimo(request.getStockMinimo());
        if (request.getStockMaximo() != null) producto.setStockMaximo(request.getStockMaximo());
        if (request.getUnidadMedida() != null) producto.setUnidadMedida(request.getUnidadMedida());

        // 7. Guardar cambios
        Producto actualizado = productoRepository.save(producto);

        return convertoToResponse(actualizado, true);
    }

    // ── Mapeador Interno y Reglas de Negocio de Visualización ─────────────────
    private ProductoResponse convertoToResponse(Producto producto, boolean isAdmin) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .referencia(producto.getReferencia())
                .ean(producto.getEan())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .categoriaId(producto.getCategoria() != null ? producto.getCategoria().getId() : null)
                .proveedorId(producto.getProveedor() != null ? producto.getProveedor().getId() : null)
                .precioVenta(producto.getPrecioVenta())
                // Criterio de aceptación clave: Precio de coste es visible únicamente para el rol Administrador
                .precioCoste(isAdmin ? producto.getPrecioCoste() : null) 
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .stockMaximo(producto.getStockMaximo())
                .enAlerta(producto.getEnAlerta() != null ? producto.getEnAlerta() : false)
                .activo(producto.getActivo())
                .build();
    }
}
