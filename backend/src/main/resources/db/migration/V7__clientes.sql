-- =============================================================
-- V7__clientes.sql
-- Tabla: clientes
-- Depende de: V1__schema_base.sql (función set_updated_at)
-- Fase 6 puede ejecutarse en paralelo a Fases 2-5 (solo depende de Fase 1)
-- =============================================================

CREATE TABLE clientes (
    id          BIGSERIAL       PRIMARY KEY,
    nombre      VARCHAR(100)    NOT NULL,
    apellidos   VARCHAR(150)    NULL,
    telefono    VARCHAR(20)     NOT NULL,
    email       VARCHAR(150)    NULL,
    nif         VARCHAR(20)     NULL,
    direccion   VARCHAR(255)    NULL,
    ciudad      VARCHAR(100)    NULL,
    notas       TEXT            NULL,
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_alta  DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Unicidad condicional: solo aplica cuando el campo tiene valor
    CONSTRAINT uq_clientes_email   UNIQUE (email),
    CONSTRAINT uq_clientes_nif     UNIQUE (nif)
);

-- ── Índices ────────────────────────────────────────────────────

-- Búsqueda desde POS por nombre parcial (debounce 300ms en frontend)
CREATE INDEX idx_clientes_nombre_apellidos
    ON clientes (nombre, apellidos);

-- Búsqueda rápida por teléfono desde POS
CREATE INDEX idx_clientes_telefono
    ON clientes (telefono);

-- Filtrado por estado activo/inactivo
CREATE INDEX idx_clientes_activo
    ON clientes (activo);

-- Ordenación por fecha de alta (listado admin)
CREATE INDEX idx_clientes_fecha_alta
    ON clientes (fecha_alta DESC);

-- ── Trigger updated_at ────────────────────────────────────────
-- Reutiliza la función set_updated_at() creada en V1__schema_base.sql
CREATE TRIGGER trg_clientes_updated_at
    BEFORE UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();