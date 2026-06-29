-- =============================================================
-- R__seed.sql
-- Datos de desarrollo para Algedro S.L.
-- Fuente: Documentation/Data-Model.md seccion 4.
--
-- Migracion repetible e idempotente: puede ejecutarse mas de una vez
-- sin duplicar registros de referencia ni la venta de ejemplo.
-- =============================================================

-- Usuarios (password de demo segun Data-Model.md: admin / admin)
INSERT INTO usuarios (username, password_hash, rol, activo)
VALUES
    ('admin', '$2a$10$v/zydARgeX43QGO9Bf5Of.6A.0po5FN9wjWtfLuyRrq3zKXEPMITe', 'ADMIN', TRUE),
    ('maria', '$2a$10$v/zydARgeX43QGO9Bf5Of.6A.0po5FN9wjWtfLuyRrq3zKXEPMITe', 'EMPLEADO', TRUE)
ON CONFLICT (username) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    rol = EXCLUDED.rol,
    activo = EXCLUDED.activo;

-- Empleados
INSERT INTO empleados (usuario_id, nombre, apellidos, dni, telefono, email, cargo, salario, fecha_contratacion)
VALUES
    ((SELECT id FROM usuarios WHERE username = 'admin'), 'Carlos', 'Garcia Lopez', '12345678A', '666111222', 'carlos@algedro.com', 'Encargado', 28000.00, DATE '2020-03-15'),
    ((SELECT id FROM usuarios WHERE username = 'maria'), 'Maria', 'Sanchez Ruiz', '87654321B', '666333444', 'maria@algedro.com', 'Dependiente', 22000.00, DATE '2022-09-01')
ON CONFLICT (usuario_id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    apellidos = EXCLUDED.apellidos,
    dni = EXCLUDED.dni,
    telefono = EXCLUDED.telefono,
    email = EXCLUDED.email,
    cargo = EXCLUDED.cargo,
    salario = EXCLUDED.salario,
    fecha_contratacion = EXCLUDED.fecha_contratacion;

-- Proveedores
INSERT INTO proveedores (nombre, nif, contacto_nombre, telefono, email, ciudad, activo)
VALUES
    ('Distribuciones Quimicas del Sur S.L.', 'B12345678', 'Pedro Martinez', '954111222', 'pedidos@dqsur.es', 'Sevilla', TRUE),
    ('ProClean Mayoristas S.A.', 'A87654321', 'Ana Fernandez', '913222333', 'comercial@proclean.es', 'Madrid', TRUE)
ON CONFLICT (nif) DO UPDATE
SET nombre = EXCLUDED.nombre,
    contacto_nombre = EXCLUDED.contacto_nombre,
    telefono = EXCLUDED.telefono,
    email = EXCLUDED.email,
    ciudad = EXCLUDED.ciudad,
    activo = EXCLUDED.activo;

-- Categorias
INSERT INTO categorias (nombre, descripcion, padre_id)
SELECT 'Limpieza del Hogar', 'Productos para limpieza domestica', NULL
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Limpieza del Hogar');

INSERT INTO categorias (nombre, descripcion, padre_id)
SELECT 'Higiene Personal', 'Productos de cuidado e higiene personal', NULL
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Higiene Personal');

INSERT INTO categorias (nombre, descripcion, padre_id)
SELECT 'Limpieza de Suelos', 'Fregonas, mopas, bayetas', id
FROM categorias
WHERE nombre = 'Limpieza del Hogar'
  AND NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Limpieza de Suelos');

INSERT INTO categorias (nombre, descripcion, padre_id)
SELECT 'Desinfectantes', 'Lejias, amoniaco, productos virucidas', id
FROM categorias
WHERE nombre = 'Limpieza del Hogar'
  AND NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Desinfectantes');

-- Productos
INSERT INTO productos (
    referencia, ean, nombre, descripcion, categoria_id, proveedor_id,
    precio_venta, precio_coste, stock_actual, stock_minimo, activo
)
VALUES
    ('LIM-001', '8410030041002', 'Fregasuelos Pino 1L', 'Limpiador de suelos con aroma a pino',
     (SELECT id FROM categorias WHERE nombre = 'Limpieza de Suelos'),
     (SELECT id FROM proveedores WHERE nif = 'B12345678'), 2.95, 1.40, 48, 10, TRUE),
    ('LIM-002', '8410030055009', 'Lejia Clasica 1.5L', 'Lejia domestica concentrada',
     (SELECT id FROM categorias WHERE nombre = 'Desinfectantes'),
     (SELECT id FROM proveedores WHERE nif = 'B12345678'), 1.65, 0.75, 92, 20, TRUE),
    ('HIG-001', '8424259101003', 'Gel de Ducha Neutro 500ml', 'Gel de bano pH neutro para piel sensible',
     (SELECT id FROM categorias WHERE nombre = 'Higiene Personal'),
     (SELECT id FROM proveedores WHERE nif = 'A87654321'), 3.50, 1.80, 30, 15, TRUE),
    ('HIG-002', '8424259102007', 'Champu Suave 400ml', 'Champu para uso diario sin parabenos',
     (SELECT id FROM categorias WHERE nombre = 'Higiene Personal'),
     (SELECT id FROM proveedores WHERE nif = 'A87654321'), 4.20, 2.10, 24, 10, TRUE),
    ('LIM-003', '8410030048001', 'Quitagrasas Cocina Spray 750ml', 'Desengrasante para superficies de cocina',
     (SELECT id FROM categorias WHERE nombre = 'Desinfectantes'),
     (SELECT id FROM proveedores WHERE nif = 'B12345678'), 3.10, 1.55, 8, 12, TRUE)
ON CONFLICT (referencia) DO UPDATE
SET ean = EXCLUDED.ean,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    categoria_id = EXCLUDED.categoria_id,
    proveedor_id = EXCLUDED.proveedor_id,
    precio_venta = EXCLUDED.precio_venta,
    precio_coste = EXCLUDED.precio_coste,
    stock_minimo = EXCLUDED.stock_minimo,
    activo = EXCLUDED.activo;

-- Clientes
INSERT INTO clientes (nombre, apellidos, telefono, email, activo, fecha_alta)
VALUES
    ('Lucia', 'Torres Mendez', '612555101', 'lucia.torres@email.com', TRUE, DATE '2023-02-10')
ON CONFLICT (email) DO UPDATE
SET nombre = EXCLUDED.nombre,
    apellidos = EXCLUDED.apellidos,
    telefono = EXCLUDED.telefono,
    activo = EXCLUDED.activo,
    fecha_alta = EXCLUDED.fecha_alta;

INSERT INTO clientes (nombre, apellidos, telefono, email, activo, fecha_alta)
SELECT 'Roberto', 'Diaz Peral', '623444202', NULL, TRUE, DATE '2024-06-20'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE telefono = '623444202');

-- Venta de ejemplo
INSERT INTO ventas (numero_venta, empleado_id, cliente_id, subtotal, descuento_global, total, metodo_pago, estado)
VALUES (
    'VTA-2024-00001',
    (SELECT e.id FROM empleados e JOIN usuarios u ON u.id = e.usuario_id WHERE u.username = 'maria'),
    (SELECT id FROM clientes WHERE email = 'lucia.torres@email.com'),
    8.10,
    0.00,
    8.10,
    'EFECTIVO',
    'COMPLETADA'
)
ON CONFLICT (numero_venta) DO UPDATE
SET empleado_id = EXCLUDED.empleado_id,
    cliente_id = EXCLUDED.cliente_id,
    subtotal = EXCLUDED.subtotal,
    descuento_global = EXCLUDED.descuento_global,
    total = EXCLUDED.total,
    metodo_pago = EXCLUDED.metodo_pago;

INSERT INTO detalle_ventas (venta_id, producto_id, nombre_producto, cantidad, precio_unitario, descuento_linea, subtotal_linea)
SELECT v.id, p.id, seed.nombre_producto, seed.cantidad, seed.precio_unitario, seed.descuento_linea, seed.subtotal_linea
FROM ventas v
CROSS JOIN (
    VALUES
        ('LIM-001', 'Fregasuelos Pino 1L', 2, 2.95::numeric, 0.00::numeric, 5.90::numeric),
        ('LIM-002', 'Lejia Clasica 1.5L', 1, 1.65::numeric, 0.00::numeric, 1.65::numeric),
        ('HIG-001', 'Gel de Ducha Neutro 500ml', 1, 3.50::numeric, 2.95::numeric, 0.55::numeric)
) AS seed(referencia, nombre_producto, cantidad, precio_unitario, descuento_linea, subtotal_linea)
JOIN productos p ON p.referencia = seed.referencia
WHERE v.numero_venta = 'VTA-2024-00001'
  AND NOT EXISTS (
      SELECT 1
      FROM detalle_ventas d
      WHERE d.venta_id = v.id
        AND d.producto_id = p.id
  );

INSERT INTO movimientos_stock (producto_id, tipo, cantidad, stock_resultante, venta_id, empleado_id)
SELECT p.id, 'VENTA', seed.cantidad, seed.stock_resultante, v.id, e.id
FROM ventas v
JOIN empleados e ON e.id = v.empleado_id
CROSS JOIN (
    VALUES
        ('LIM-001', -2, 46),
        ('LIM-002', -1, 91),
        ('HIG-001', -1, 29)
) AS seed(referencia, cantidad, stock_resultante)
JOIN productos p ON p.referencia = seed.referencia
WHERE v.numero_venta = 'VTA-2024-00001'
  AND NOT EXISTS (
      SELECT 1
      FROM movimientos_stock m
      WHERE m.venta_id = v.id
        AND m.producto_id = p.id
        AND m.tipo = 'VENTA'
  );

UPDATE productos SET stock_actual = 46 WHERE referencia = 'LIM-001';
UPDATE productos SET stock_actual = 91 WHERE referencia = 'LIM-002';
UPDATE productos SET stock_actual = 29 WHERE referencia = 'HIG-001';
