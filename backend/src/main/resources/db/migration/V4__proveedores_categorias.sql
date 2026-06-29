-- =============================================================
-- V4__proveedores_categorias.sql
-- Fase 3 — Algedro S.L.
-- Fuente: Data-Model.md §1.3 y §1.4
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- PROVEEDORES
-- Data-Model.md §1.3
-- ─────────────────────────────────────────────────────────────

CREATE TABLE proveedores (
    id              BIGSERIAL       PRIMARY KEY,
    nombre          VARCHAR(150)    NOT NULL,
    nif             VARCHAR(20)     UNIQUE,                        -- NULL permitido; si existe, es único
    contacto_nombre VARCHAR(100),
    telefono        VARCHAR(20),
    email           VARCHAR(150),
    direccion       VARCHAR(255),
    ciudad          VARCHAR(100),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    notas           TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Índices de proveedores
CREATE INDEX idx_proveedores_nombre ON proveedores (nombre);
CREATE INDEX idx_proveedores_activo ON proveedores (activo);

-- Trigger updated_at (reutiliza la función creada en V2)
CREATE TRIGGER trg_proveedores_updated_at
    BEFORE UPDATE ON proveedores
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────────────────────────────────────────────────────
-- CATEGORIAS
-- Data-Model.md §1.4
-- NOTA: la restricción de máximo 2 niveles se valida en
--       CategoriaService.java (A7), no en BD.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE categorias (
    id          BIGSERIAL       PRIMARY KEY,
    nombre      VARCHAR(100)    NOT NULL,
    descripcion VARCHAR(255),
    padre_id    BIGINT          REFERENCES categorias(id) ON DELETE RESTRICT,
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Nivel de BD: impide auto-referencia directa
    CONSTRAINT chk_categorias_no_self_ref CHECK (padre_id IS NULL OR padre_id <> id)
);

-- Índices de categorias
CREATE INDEX idx_categorias_padre_id ON categorias (padre_id);