-- =============================================================
-- V6__movimientos_stock.sql
-- Tabla: movimientos_stock
-- Depende de: V3__empleados.sql, V4__proveedores_categorias.sql, V5__productos.sql
-- =============================================================

CREATE TABLE movimientos_stock (
    id               BIGSERIAL       PRIMARY KEY,
    producto_id      BIGINT          NOT NULL,
    tipo             VARCHAR(30)     NOT NULL,
    cantidad         INTEGER         NOT NULL,
    stock_resultante INTEGER         NOT NULL,
    motivo           TEXT            NULL,
    venta_id         BIGINT          NULL,
    proveedor_id     BIGINT          NULL,
    albaran          VARCHAR(100)    NULL,
    empleado_id      BIGINT          NOT NULL,
    fecha            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mov_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES proveedores (id) ON DELETE SET NULL,
    CONSTRAINT fk_mov_empleado
        FOREIGN KEY (empleado_id) REFERENCES empleados (id) ON DELETE RESTRICT,

    CONSTRAINT chk_mov_tipo
        CHECK (tipo IN ('ENTRADA', 'VENTA', 'AJUSTE', 'ANULACION_VENTA', 'INVENTARIO')),
    CONSTRAINT chk_mov_cantidad
        CHECK (cantidad <> 0),
    CONSTRAINT chk_mov_stock_resultante
        CHECK (stock_resultante >= 0),
    CONSTRAINT chk_mov_ajuste_motivo
        CHECK (tipo NOT IN ('AJUSTE', 'INVENTARIO') OR length(trim(coalesce(motivo, ''))) >= 10)
);

-- venta_id se indexa desde Fase 5, pero la FK se aÃ±adira en Fase 7 cuando exista ventas.
CREATE INDEX idx_mov_stock_producto_id ON movimientos_stock (producto_id);
CREATE INDEX idx_mov_stock_fecha       ON movimientos_stock (fecha);
CREATE INDEX idx_mov_stock_tipo        ON movimientos_stock (tipo);
CREATE INDEX idx_mov_stock_venta_id    ON movimientos_stock (venta_id);
CREATE INDEX idx_mov_stock_empleado_id ON movimientos_stock (empleado_id);
