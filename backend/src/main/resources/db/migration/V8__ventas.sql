-- =============================================================
-- V8__ventas.sql
-- Migración: tablas ventas + detalle_ventas
-- Trigger: generación automática de numero_venta (ADR-010)
-- Formato: VTA-YYYY-NNNNN  (ej: VTA-2024-00001)
-- Prerrequisito: V7__clientes.sql ejecutada correctamente
-- =============================================================


-- -------------------------------------------------------------
-- FUNCIÓN AUXILIAR: set_updated_at (reutilizada de otras tablas)
-- Solo se crea si no existe ya en el esquema
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


-- =============================================================
-- TABLA: ventas
-- Cabecera de cada transacción registrada en el POS.
-- El pago ya fue cobrado físicamente; el sistema solo registra.
-- =============================================================

CREATE TABLE ventas (
    id                BIGSERIAL       NOT NULL,
    numero_venta      VARCHAR(20)     NOT NULL,
    empleado_id       BIGINT          NOT NULL,
    cliente_id        BIGINT,
    fecha             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    subtotal          NUMERIC(10,2)   NOT NULL,
    descuento_global  NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    total             NUMERIC(10,2)   NOT NULL,
    metodo_pago       VARCHAR(20)     NOT NULL,
    estado            VARCHAR(20)     NOT NULL DEFAULT 'COMPLETADA',
    motivo_anulacion  TEXT,
    anulada_por       BIGINT,
    fecha_anulacion   TIMESTAMPTZ,
    notas             TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Clave primaria
    CONSTRAINT pk_ventas PRIMARY KEY (id),

    -- Número de venta legible: único y no nulo (llenado por trigger)
    CONSTRAINT uq_ventas_numero_venta UNIQUE (numero_venta),

    -- Integridad referencial
    CONSTRAINT fk_ventas_empleado
        FOREIGN KEY (empleado_id) REFERENCES empleados(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ventas_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_ventas_anulada_por
        FOREIGN KEY (anulada_por) REFERENCES empleados(id)
        ON DELETE RESTRICT,

    -- Valores permitidos para método de pago
    CONSTRAINT chk_ventas_metodo_pago
        CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'OTRO')),

    -- Valores permitidos para estado
    CONSTRAINT chk_ventas_estado
        CHECK (estado IN ('COMPLETADA', 'ANULADA')),

    -- Coherencia de anulación: si estado='ANULADA', los tres campos de anulación
    -- deben estar presentes; si estado='COMPLETADA', deben ser nulos.
    CONSTRAINT chk_ventas_anulacion CHECK (
        (estado = 'ANULADA'
            AND motivo_anulacion IS NOT NULL
            AND anulada_por      IS NOT NULL
            AND fecha_anulacion  IS NOT NULL)
        OR
        (estado = 'COMPLETADA'
            AND motivo_anulacion IS NULL
            AND anulada_por      IS NULL
            AND fecha_anulacion  IS NULL)
    ),

    -- Importes no negativos
    CONSTRAINT chk_ventas_subtotal        CHECK (subtotal        >= 0),
    CONSTRAINT chk_ventas_descuento       CHECK (descuento_global >= 0),
    CONSTRAINT chk_ventas_total           CHECK (total            >= 0)
);

-- Índices de ventas
-- UNIQUE sobre numero_venta ya se crea implícitamente por el constraint,
-- pero se añade nombre explícito para claridad en EXPLAIN y mantenimiento.
CREATE UNIQUE INDEX idx_ventas_numero_venta   ON ventas (numero_venta);
CREATE        INDEX idx_ventas_empleado_id    ON ventas (empleado_id);
CREATE        INDEX idx_ventas_cliente_id     ON ventas (cliente_id);
CREATE        INDEX idx_ventas_fecha          ON ventas (fecha);
CREATE        INDEX idx_ventas_estado         ON ventas (estado);
-- Índice compuesto para la consulta más habitual: historial filtrado por fecha + estado
CREATE        INDEX idx_ventas_fecha_estado   ON ventas (fecha, estado);


-- =============================================================
-- TABLA: detalle_ventas
-- Líneas individuales de cada venta.
-- precio_unitario y nombre_producto son snapshots del momento
-- de la venta (ADR-002 / Data-Model §5.2): cambios futuros en
-- el catálogo no alteran el historial.
-- =============================================================

CREATE TABLE detalle_ventas (
    id               BIGSERIAL       NOT NULL,
    venta_id         BIGINT          NOT NULL,
    producto_id      BIGINT          NOT NULL,
    nombre_producto  VARCHAR(150)    NOT NULL,
    cantidad         INTEGER         NOT NULL,
    precio_unitario  NUMERIC(10,2)   NOT NULL,
    descuento_linea  NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    subtotal_linea   NUMERIC(10,2)   NOT NULL,

    CONSTRAINT pk_detalle_ventas PRIMARY KEY (id),

    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (venta_id) REFERENCES ventas(id)
        ON DELETE CASCADE,       -- si se elimina la venta (solo en dev), cascada limpia las líneas

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (producto_id) REFERENCES productos(id)
        ON DELETE RESTRICT,      -- no se puede eliminar un producto con historial de ventas

    CONSTRAINT chk_detalle_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_precio
        CHECK (precio_unitario > 0),
    CONSTRAINT chk_detalle_descuento
        CHECK (descuento_linea >= 0),
    CONSTRAINT chk_detalle_subtotal_linea
        CHECK (subtotal_linea >= 0)
);

-- Índices de detalle_ventas
CREATE INDEX idx_detalle_ventas_venta_id    ON detalle_ventas (venta_id);
CREATE INDEX idx_detalle_ventas_producto_id ON detalle_ventas (producto_id);


-- =============================================================
-- TRIGGER: generación automática de numero_venta (ADR-010)
--
-- Formato: VTA-YYYY-NNNNN
--   · VTA     — prefijo fijo del dominio "venta"
--   · YYYY    — año natural de la fecha de la venta (4 dígitos)
--   · NNNNN   — secuencia correlativa DENTRO del año, con zero-padding a 5 dígitos
--
-- Ejemplos:
--   primera venta del 2024  → VTA-2024-00001
--   venta 99999 del 2025    → VTA-2025-99999
--   primera venta del 2026  → VTA-2026-00001  (el contador reinicia por año)
--
-- Comportamiento:
--   1. Se dispara BEFORE INSERT sobre ventas.
--   2. Si numero_venta ya tiene valor (ej: seed o test con valor explícito),
--      el trigger lo respeta y no sobreescribe.
--   3. Si numero_venta es NULL, calcula el siguiente número del año en curso:
--      cuenta cuántas ventas existen ya para ese año y suma 1.
--   4. La consulta de MAX + 1 dentro de la misma transacción es segura con
--      el nivel de aislamiento READ COMMITTED de PostgreSQL para cargas normales
--      de una droguería (volumen bajo, sin picos de concurrencia masiva).
--      Para sistemas de alto volumen se recomendaría una secuencia por año,
--      pero aquí la simplicidad prima (ADR-010 justificado en §5.10 del Data-Model).
-- =============================================================

CREATE OR REPLACE FUNCTION fn_generar_numero_venta()
RETURNS TRIGGER
LANGUAGE plpgsql AS
$$
DECLARE
    v_anio        INTEGER;
    v_siguiente   INTEGER;
    v_numero      VARCHAR(20);
BEGIN
    -- Si ya viene informado (ej: datos de seed o test explícito), no tocar
    IF NEW.numero_venta IS NOT NULL AND NEW.numero_venta <> '' THEN
        RETURN NEW;
    END IF;

    -- Año de la venta: si fecha viene informada la usamos; si no, NOW()
    v_anio := EXTRACT(YEAR FROM COALESCE(NEW.fecha, NOW()));

    -- Siguiente número correlativo para ese año
    -- Patrón: 'VTA-YYYY-%' filtra todas las ventas del mismo año
    SELECT COALESCE(
               MAX(
                   CAST(
                       SPLIT_PART(numero_venta, '-', 3) AS INTEGER
                   )
               ),
               0
           ) + 1
    INTO v_siguiente
    FROM ventas
    WHERE numero_venta LIKE 'VTA-' || v_anio || '-%';

    -- Formatear con zero-padding a 5 dígitos (FM evita espacios en TO_CHAR)
    v_numero := 'VTA-' || v_anio || '-' || LPAD(v_siguiente::TEXT, 5, '0');

    NEW.numero_venta := v_numero;
    RETURN NEW;
END;
$$;

-- Asociar la función al trigger BEFORE INSERT en ventas
CREATE TRIGGER trg_ventas_numero_venta
    BEFORE INSERT
    ON ventas
    FOR EACH ROW
    EXECUTE FUNCTION fn_generar_numero_venta();


-- =============================================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- Facilitan la comprensión en pgAdmin y en INFORMATION_SCHEMA
-- =============================================================

COMMENT ON TABLE  ventas                       IS 'Cabecera de ventas del POS. Un registro por transacción.';
COMMENT ON COLUMN ventas.numero_venta          IS 'Número legible del ticket. Formato VTA-YYYY-NNNNN. Generado por trg_ventas_numero_venta (ADR-010).';
COMMENT ON COLUMN ventas.subtotal              IS 'Suma de subtotal_linea de todas las líneas antes del descuento global.';
COMMENT ON COLUMN ventas.descuento_global      IS 'Descuento en euros aplicado sobre el subtotal. Excluyente con descuentos por línea.';
COMMENT ON COLUMN ventas.total                 IS 'Importe final = subtotal - descuento_global. Calculado en la capa de aplicación.';
COMMENT ON COLUMN ventas.estado                IS 'COMPLETADA (estado inicial) | ANULADA (solo por ADMIN con motivo).';
COMMENT ON COLUMN ventas.motivo_anulacion      IS 'Texto libre obligatorio cuando estado = ANULADA.';
COMMENT ON COLUMN ventas.anulada_por           IS 'FK al empleado ADMIN que ejecutó la anulación.';

COMMENT ON TABLE  detalle_ventas               IS 'Líneas de venta. precio_unitario y nombre_producto son snapshots del momento de la venta (ADR-002).';
COMMENT ON COLUMN detalle_ventas.nombre_producto IS 'Snapshot del nombre del producto en el momento de la venta. Inmutable.';
COMMENT ON COLUMN detalle_ventas.precio_unitario IS 'PVP vigente en el momento de la venta. No varía aunque cambie el catálogo.';
COMMENT ON COLUMN detalle_ventas.subtotal_linea  IS '(cantidad × precio_unitario) − descuento_linea. Calculado en la capa de aplicación.';

COMMENT ON FUNCTION fn_generar_numero_venta()  IS 'Genera numero_venta con formato VTA-YYYY-NNNNN. Se invoca via trg_ventas_numero_venta. ADR-010.';
COMMENT ON TRIGGER  trg_ventas_numero_venta ON ventas IS 'BEFORE INSERT: asigna numero_venta si es NULL. Respeta valor explícito (seed/tests).';