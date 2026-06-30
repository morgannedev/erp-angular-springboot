# Algedro S.L.

Aplicación web de gestión integral para droguería. Centraliza el control de inventario, el registro de ventas en punto de venta (POS), y la gestión de clientes, proveedores y empleados en una única herramienta interna accesible solo para el personal autorizado.

<img width="1600" height="736" alt="image" src="https://github.com/user-attachments/assets/3b2371b7-a476-43f3-a335-da4c47876bbc" />

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | Angular 17 (SPA) |
| Backend | Spring Boot 3 · Java 17 |
| Base de datos | PostgreSQL 16 |
| Infraestructura | Docker Compose |
| Autenticación | JWT (Spring Security) |
| Migraciones | Flyway |
| Tests | JUnit 5 · Mockito · Testcontainers |
| Metodología | TDD estricto |

---

## Requisitos previos

- Docker Desktop
- Java 17 y Maven (solo para desarrollo local)
- Node.js 20 (solo para desarrollo local)

---

## Inicio rápido con Docker

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/algedro-erp.git
cd algedro-erp
```

### 2. Crear el archivo de entorno

```bash
# Linux / macOS
cp .env.example .env

# Windows (PowerShell)
Copy-Item .env.example .env
```

### 3. Editar `.env`

```env
POSTGRES_HOST=db
POSTGRES_PORT=5432
POSTGRES_DB=algedro
POSTGRES_USER=algedro_user
POSTGRES_PASSWORD=una_password_segura
JWT_SECRET=un_secreto_largo_de_al_menos_32_caracteres
SPRING_PROFILES_ACTIVE=prod
FRONTEND_PORT=80
```

### 4. Arrancar los servicios

```bash
docker compose -f docker-compose.prod.yml --env-file .env up --build
```

La aplicación quedará disponible en `http://localhost`.

### 5. Detener los servicios

```bash
# Detener sin borrar datos
docker compose -f docker-compose.prod.yml down

# Detener y borrar volúmenes
docker compose -f docker-compose.prod.yml down -v
```

---

## Desarrollo local

### 1. Levantar PostgreSQL

```bash
docker compose up -d
```

Servicios disponibles:
- PostgreSQL: `localhost:5432`
- pgAdmin: `http://localhost:5050`

### 2. Arrancar el backend

```bash
cd backend
mvn spring-boot:run
```

API disponible en `http://localhost:8080/api/v1`.

### 3. Arrancar el frontend

```bash
cd frontend
npm ci
npm start
```

Frontend disponible en `http://localhost:4200`.

---

## Tests

El proyecto sigue TDD estricto: los tests se escriben antes que la implementación. Estado verificado: **222 tests, 0 fallos**.

```bash
cd backend
mvn test
```

Los tests de integración dependen de Testcontainers y requieren Docker activo. En entornos sin Docker, 2 tests se omiten automáticamente.

<img width="735" height="569" alt="image" src="https://github.com/user-attachments/assets/e612964f-b1ed-4e52-9e0d-03c3c3d5b62f" />

---

## Credenciales de demo

El seed de datos (`R__seed.sql`) crea los siguientes usuarios de prueba:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `maria` | `admin123` | EMPLEADO |

> Antes de exponer el proyecto en internet, cambiar `JWT_SECRET`, `POSTGRES_PASSWORD` y las contraseñas de los usuarios demo.

---

## API REST

Todas las rutas se exponen bajo el prefijo `/api/v1`:

```
/api/v1/auth
/api/v1/productos
/api/v1/categorias
/api/v1/stock
/api/v1/ventas
/api/v1/clientes
/api/v1/proveedores
/api/v1/empleados
```

---

## Módulos

### Autenticación

Login con JWT. La sesión expira tras 8 horas de inactividad o al cerrar el navegador. Bloqueo temporal de cuenta tras 5 intentos fallidos. El administrador puede forzar el cierre de sesión de cualquier empleado.

<img width="1600" height="736" alt="image" src="https://github.com/user-attachments/assets/09a8aa71-67e2-45f5-9709-61645550dbc3" />

### POS — Punto de Venta

Registro de ventas en tiempo real con búsqueda por nombre, referencia o código de barras (compatible con lectores). Soporta descuentos por línea y globales, asociación opcional de cliente y selección de método de pago. Genera ticket exportable a PDF.

<img width="1600" height="729" alt="image" src="https://github.com/user-attachments/assets/713c7349-d6d7-4957-afec-bad69acb09b1" />

### Historial de Ventas

Listado paginado y filtrable por fecha, empleado y método de pago. El rol ADMIN puede exportar a CSV y anular ventas; la anulación revierte automáticamente el stock.

<img width="1600" height="732" alt="image" src="https://github.com/user-attachments/assets/f6285a92-4a1b-4447-aa4c-5e3ac0384bdf" />

### Productos y Categorías

Catálogo con nombre, SKU, EAN, PVP, precio de coste, categoría, proveedor y estado (activo/inactivo). Categorías con hasta dos niveles de jerarquía. El precio de coste es visible solo para el rol ADMIN.

### Stock

Niveles en tiempo real con alertas de stock mínimo en el dashboard del administrador. Registro de entradas de mercancía vinculadas a proveedor y albarán. Ajustes manuales con motivo obligatorio. Historial completo de movimientos por producto.

### Clientes

Base de datos con historial de compras. Alta rápida desde el POS con solo nombre y teléfono como campos obligatorios. Exportación a CSV restringida al rol ADMIN.

### Proveedores

Gestión de proveedores vinculados a productos y entradas de stock.

### Empleados

Creación de cuentas, asignación de roles (ADMIN / EMPLEADO), activación y desactivación de cuentas, y cierre forzado de sesión. Accesible solo para el rol ADMIN.

---

## Roles de usuario

El sistema tiene dos roles internos. No existe registro público.

**ADMIN**

Acceso completo a todos los módulos: CRUD de productos, categorías, stock, proveedores, clientes y empleados. Anulación de ventas. Exportación de datos. Ajustes de inventario. Cierre forzado de sesiones.

**EMPLEADO**

Acceso operativo: registro de ventas (POS), consulta del catálogo y niveles de stock (solo lectura), búsqueda y creación de clientes, consulta de proveedores, historial de sus propias ventas.

---

## Estructura del proyecto

```
algedro-erp/
├── backend/                        # Spring Boot (Java 17)
│   ├── src/main/java/com/algedro/
│   │   ├── auth/
│   │   ├── producto/
│   │   ├── categoria/
│   │   ├── stock/
│   │   ├── venta/
│   │   ├── cliente/
│   │   ├── proveedor/
│   │   └── empleado/
│   ├── src/main/resources/db/migration/
│   │   ├── V1__schema_base.sql
│   │   ├── V2__usuarios.sql
│   │   ├── V3__empleados.sql
│   │   ├── V4__proveedores_categorias.sql
│   │   ├── V5__productos.sql
│   │   ├── V6__movimientos_stock.sql
│   │   ├── V7__clientes.sql
│   │   ├── V8__ventas.sql
│   │   └── R__seed.sql
│   └── Dockerfile
│
├── frontend/                       # Angular 17 (SPA)
│   ├── src/app/
│   │   ├── core/auth/
│   │   ├── features/
│   │   │   ├── dashboard/
│   │   │   ├── pos/
│   │   │   ├── productos/
│   │   │   ├── stock/
│   │   │   ├── ventas/
│   │   │   ├── clientes/
│   │   │   ├── proveedores/
│   │   │   └── empleados/
│   │   └── shared/
│   └── Dockerfile
│
├── Documentation/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
├── .gitignore
└── .github/
    └── workflows/
        └── ci.yml
```
