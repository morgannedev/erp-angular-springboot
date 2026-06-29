CREATE TABLE empleados (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    dni VARCHAR(20) NULL,
    telefono VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    cargo VARCHAR(100) NOT NULL,
    salario NUMERIC(10,2) NULL,
    fecha_contratacion DATE NOT NULL,
    fecha_baja DATE NULL,
    notas TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_empleados_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT uq_empleados_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_empleados_dni UNIQUE (dni),
    CONSTRAINT uq_empleados_email UNIQUE (email),
    CONSTRAINT chk_empleados_salario CHECK (salario IS NULL OR salario > 0),
    CONSTRAINT chk_empleados_fechas CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_contratacion)
);

CREATE INDEX idx_empleados_usuario_id ON empleados (usuario_id);
CREATE INDEX idx_empleados_apellidos_nombre ON empleados (apellidos, nombre);

CREATE TRIGGER trg_empleados_updated_at
    BEFORE UPDATE ON empleados
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
