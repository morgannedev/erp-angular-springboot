-- =============================================================
-- V5__productos.sql
-- Tabla: productos
-- Depende de: V4__proveedores_categorias.sql
-- =============================================================

-- Extensión pg_trgm necesaria para el índice GIN de búsqueda POS
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE productos (
    id              BIGSERIAL       PRIMARY KEY,
    referencia      VARCHAR(50)     NOT NULL,
    ean             VARCHAR(20)     NULL,
    nombre          VARCHAR(150)    NOT NULL,
    descripcion     TEXT            NULL,
    categoria_id    BIGINT          NOT NULL
                        REFERENCES categorias(id) ON DELETE RESTRICT,
    proveedor_id    BIGINT          NULL
                        REFERENCES proveedores(id) ON DELETE SET NULL,
    precio_venta    NUMERIC(10,2)   NOT NULL,
    precio_coste    NUMERIC(10,2)   NULL,
    unidad_medida   VARCHAR(30)     NOT NULL DEFAULT 'ud',
    stock_actual    INTEGER         NOT NULL DEFAULT 0,
    stock_minimo    INTEGER         NOT NULL DEFAULT 0,
    stock_maximo    INTEGER         NULL,
    en_alerta       BOOLEAN         NOT NULL DEFAULT FALSE,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    imagen_url      VARCHAR(500)    NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_productos_referencia  UNIQUE (referencia),
    CONSTRAINT uq_productos_ean         UNIQUE (ean),

    CONSTRAINT chk_productos_precio_venta CHECK (precio_venta > 0),
    CONSTRAINT chk_productos_precio_coste CHECK (precio_coste IS NULL OR precio_coste > 0),
    CONSTRAINT chk_productos_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT chk_productos_stock_min    CHECK (stock_minimo >= 0),
    CONSTRAINT chk_productos_stock_max    CHECK (stock_maximo IS NULL OR stock_maximo > stock_minimo)
);

-- ── Índices ────────────────────────────────────────────────────

-- Búsqueda POS: ILIKE '%texto%' — requiere GIN + pg_trgm
CREATE INDEX idx_productos_nombre_gin
    ON productos USING GIN (nombre gin_trgm_ops);

-- Búsqueda por código de barras (lector / teclado rápido)
CREATE UNIQUE INDEX idx_productos_ean
    ON productos (ean)
    WHERE ean IS NOT NULL;

-- SKU único (ya cubierto por constraint, pero explícito para performance)
CREATE UNIQUE INDEX idx_productos_referencia
    ON productos (referencia);

-- FK lookups y filtros frecuentes
CREATE INDEX idx_productos_categoria_id  ON productos (categoria_id);
CREATE INDEX idx_productos_proveedor_id  ON productos (proveedor_id);

-- Alerta de stock mínimo — consulta del dashboard ADMIN
CREATE INDEX idx_productos_activo_stock  ON productos (activo, stock_actual);

-- ── Trigger updated_at ────────────────────────────────────────
-- Reutiliza la función set_updated_at() creada en migraciones anteriores
CREATE TRIGGER trg_productos_updated_at
    BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
