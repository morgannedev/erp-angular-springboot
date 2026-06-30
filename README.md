# Algedro S.L.

Aplicacion web de gestion integral para una drogueria pequena o mediana. Centraliza autenticacion interna, punto de venta, inventario, catalogo de productos, categorias, clientes, proveedores, empleados e historial de ventas en una unica herramienta privada para personal autorizado.

El proyecto esta construido como una aplicacion full stack con Angular, Spring Boot y PostgreSQL. La documentacion inicial aportada para crear el sistema se ha usado como base funcional y tecnica: modelo de datos, contrato OpenAPI, diagramas de secuencia, guia de despliegue y ADRs de arquitectura.

<img width="1600" height="736" alt="image" src="https://github.com/user-attachments/assets/3b2371b7-a476-43f3-a335-da4c47876bbc" />

---

## Objetivo del proyecto

Algedro S.L. resuelve una necesidad habitual en comercios minoristas: sustituir hojas de calculo, apuntes manuales y procesos dispersos por una herramienta unica de gestion operativa.

El sistema permite:

- Registrar ventas desde un POS web.
- Descontar stock automaticamente al vender.
- Revertir stock cuando una venta se anula.
- Consultar el historial de ventas por fecha, empleado, cliente, estado y metodo de pago.
- Gestionar productos, categorias y proveedores.
- Mantener clientes con historial de compras.
- Administrar empleados y cuentas de acceso.
- Controlar niveles de inventario, entradas de mercancia, ajustes y alertas de stock minimo.
- Mantener trazabilidad historica de ventas y movimientos de stock.

No es una tienda online ni una aplicacion publica para clientes. Es una herramienta interna para el personal autorizado de la drogueria.

---

## Decisiones de arquitectura

- `usuarios` y `empleados` separados para no mezclar seguridad con datos laborales.
- `detalle_ventas` guarda snapshot de nombre y precio para preservar historico.
- `movimientos_stock` funciona como auditoria de solo insercion.
- Enums como `VARCHAR` + `CHECK` para evolucionar con Flyway.
- Dinero en `NUMERIC(10,2)` y Java `BigDecimal`.
- Fechas con hora en `TIMESTAMPTZ`.
- Busqueda POS con `pg_trgm` e indice GIN.
- Numero de venta legible `VTA-YYYY-NNNNN`, generado por trigger.

---

## Stack tecnológico

| Capa | Tecnologia | Uso en el proyecto |
|---|---|---|
| Frontend | Angular 17 | SPA con rutas protegidas, servicios HTTP y modulos funcionales |
| Backend | Spring Boot 3.3.5 | API REST, seguridad, reglas de negocio y transacciones |
| Lenguaje backend | Java 17 | Servicios, controladores, entidades JPA y tests |
| Base de datos | PostgreSQL 16 | Persistencia relacional, constraints, indices y triggers |
| Seguridad | Spring Security + JWT | Login, roles, filtros JWT, blacklist y refresh tokens |
| Migraciones | Flyway | Versionado del esquema y seed de datos |
| ORM | Spring Data JPA / Hibernate | Repositorios y entidades de dominio |
| Mapeo | MapStruct | Conversion entre entidades y DTOs |
| Validacion | Jakarta Validation | Validacion de entrada en DTOs |
| Frontend PDF | jsPDF + html2canvas | Generacion/exportacion de tickets desde la interfaz |
| Infraestructura | Docker Compose + Nginx | PostgreSQL, pgAdmin, backend y frontend |
| Tests backend | JUnit 5, Mockito, MockMvc, Testcontainers | Unitarios, slice tests e integracion |
| Tests frontend | Jasmine/Karma | Tests unitarios Angular |

---

## Arquitectura general

El proyecto sigue una arquitectura por capas, organizada por modulos de negocio. En el backend, cada dominio agrupa controller, service, repository, DTOs y entidades relacionadas. En el frontend, cada area funcional vive dentro de `features/`, con modelos y autenticacion compartidos en `core/`.

```text
Angular SPA
  |
  | HTTP REST + Bearer JWT
  v
Spring Boot API
  |
  | Spring Data JPA / JDBC
  v
PostgreSQL
```

Flujo principal:

1. El usuario inicia sesion en Angular.
2. El backend valida credenciales con Spring Security y bcrypt.
3. La API devuelve access token JWT y refresh token.
4. Angular usa el token para consumir rutas protegidas.
5. Spring Security valida cada peticion y aplica permisos por rol.
6. Las operaciones de negocio se ejecutan en servicios transaccionales.
7. PostgreSQL garantiza integridad con constraints, claves foraneas, indices y triggers.

---

## Modulos funcionales

### Autenticacion y sesion

La aplicacion no tiene registro publico. Las cuentas se crean desde el modulo de empleados por un administrador.

Funciones implementadas:

- Login con usuario y contrasena.
- Hash de contrasenas con bcrypt.
- JWT para autenticar peticiones.
- Refresh tokens persistidos en base de datos.
- Logout con invalidacion de access token mediante blacklist.
- Bloqueo temporal por intentos fallidos.
- Endpoint `/auth/me` para consultar la identidad autenticada.

<img width="1600" height="736" alt="Pantalla de login" src="https://github.com/user-attachments/assets/09a8aa71-67e2-45f5-9709-61645550dbc3" />

### Dashboard

Pantalla inicial tras iniciar sesion. Resume accesos a los modulos principales y permite al administrador detectar rapidamente productos por debajo del stock minimo.

El contenido cambia segun rol:

- `ADMIN`: vision completa, accesos de administracion y alertas de stock.
- `EMPLEADO`: accesos operativos a ventas, catalogo y consulta.

### POS - Punto de venta

El POS permite registrar ventas en tienda desde una interfaz web.

Capacidades principales:

- Busqueda de productos por nombre, referencia o codigo de barras.
- Compatibilidad con lectores de codigo de barras que actuan como teclado.
- Lineas de venta con cantidad, precio, descuento y subtotal.
- Cliente opcional asociado a la venta.
- Creacion rapida de cliente desde el flujo de venta.
- Metodo de pago: efectivo, tarjeta u otro.
- Calculo de subtotal, descuentos y total.
- Registro de venta en backend.
- Descuento automatico de stock.
- Creacion de movimientos de stock de tipo `VENTA`.
- Generacion de ticket exportable.

<img width="1600" height="729" alt="POS punto de venta" src="https://github.com/user-attachments/assets/713c7349-d6d7-4957-afec-bad69acb09b1" />

### Historial de ventas

Modulo para consultar ventas registradas y revisar sus lineas.

Incluye:

- Listado paginado.
- Filtros por fecha, empleado, metodo de pago y estado.
- Detalle de venta con lineas.
- Numero de venta legible.
- Estados `COMPLETADA` y `ANULADA`.
- Anulacion de ventas por administrador.
- Reversion automatica del stock al anular.
- Registro de movimiento `ANULACION_VENTA`.

<img width="1600" height="732" alt="Historial de ventas" src="https://github.com/user-attachments/assets/f6285a92-4a1b-4447-aa4c-5e3ac0384bdf" />

### Productos

Catalogo central de articulos vendidos por la drogueria.

Cada producto contiene:

- Referencia interna o SKU.
- Codigo EAN opcional.
- Nombre y descripcion.
- Categoria.
- Proveedor principal opcional.
- Precio de venta.
- Precio de coste.
- Unidad de medida.
- Stock actual.
- Stock minimo y maximo.
- Estado activo/inactivo.
- URL de imagen opcional.

El precio de coste es informacion sensible y debe tratarse como dato de administracion.

<img width="1600" height="723" alt="Catalogo de productos" src="https://github.com/user-attachments/assets/30fd9262-4e3c-4d6f-94f7-f160e01bcd96" />

### Categorias

Las categorias organizan el catalogo en una jerarquia sencilla de dos niveles:

- Categoria raiz.
- Subcategoria.

La base de datos evita autoreferencias directas y la regla de maximo dos niveles se valida en la capa de servicio. Esta decision deja el esquema flexible si en el futuro el negocio necesita mas profundidad.

<img width="1600" height="734" alt="Gestion de categorias" src="https://github.com/user-attachments/assets/78aa2f30-0550-4571-af81-e2e7ef5f7401" />

### Stock

El modulo de stock combina consulta operativa y trazabilidad.

Incluye:

- Consulta de niveles actuales.
- Alertas de productos bajo minimo.
- Registro de entradas de mercancia.
- Ajustes manuales con motivo obligatorio.
- Historial de movimientos por producto.
- Stock resultante guardado en cada movimiento.
- Asociacion opcional de entradas con proveedor y albaran.

El sistema usa doble escritura controlada:

- `productos.stock_actual` permite consultas rapidas en POS y dashboard.
- `movimientos_stock` guarda el historial auditable de cada variacion.

<img width="1600" height="736" alt="Modulo de stock" src="https://github.com/user-attachments/assets/68fb1179-9552-4283-8215-6adc291a2e60" />

### Clientes

Modulo para gestionar clientes habituales y asociarlos a ventas.

Permite:

- Buscar por nombre, apellidos o telefono.
- Crear clientes desde el POS o desde el modulo de clientes.
- Consultar ficha del cliente.
- Mantener datos de contacto y notas.
- Activar/desactivar clientes.
- Consultar historial de compras.

<img width="1600" height="732" alt="Gestion de clientes" src="https://github.com/user-attachments/assets/373083d1-41c1-4602-8d48-bcf46cb9e525" />

### Proveedores

Modulo para gestionar proveedores de mercancia.

Permite:

- Alta y edicion de proveedor.
- Datos de contacto.
- NIF/CIF unico si se informa.
- Activacion/desactivacion.
- Vinculacion con productos.
- Asociacion con entradas de stock.

<img width="1600" height="731" alt="Gestion de proveedores" src="https://github.com/user-attachments/assets/97cb1d53-0652-44e8-9fcd-3639f072a890" />

### Empleados

Modulo de administracion interna. Relaciona los datos laborales del empleado con su cuenta de usuario.

Permite:

- Crear empleados.
- Asignar usuario y rol.
- Editar datos.
- Activar o desactivar cuenta.
- Cambiar rol.
- Eliminar cuando las reglas de integridad lo permiten.

<img width="1600" height="731" alt="Gestion de empleados" src="https://github.com/user-attachments/assets/e994f7bf-6cc5-43d3-8bd2-6577edcf5702" />

---

## Roles y permisos

El sistema contempla dos roles: `ADMIN` y `EMPLEADO`.

| Modulo / Accion | ADMIN | EMPLEADO |
|---|:---:|:---:|
| Iniciar sesion | Si | Si |
| Consultar dashboard | Si | Si |
| Gestionar empleados | Si | No |
| Consultar productos | Si | Si |
| Crear/editar/desactivar productos | Si | No |
| Ver precio de coste | Si | No |
| Gestionar categorias | Si | No |
| Consultar proveedores | Si | Si |
| Crear/editar/desactivar proveedores | Si | No |
| Consultar stock | Si | Si |
| Registrar entradas de stock | Si | No |
| Hacer ajustes de stock | Si | No |
| Consultar historial de movimientos | Si | No |
| Registrar ventas POS | Si | Si |
| Consultar ventas propias | Si | Si |
| Consultar ventas de otros empleados | Si | No |
| Anular ventas | Si | No |
| Crear clientes | Si | Si |
| Editar/desactivar clientes | Si | No |

---

## Modelo de datos

Entidades principales:

- `usuarios`: credenciales, rol, estado, intentos fallidos y bloqueo.
- `refresh_tokens`: tokens de refresco activos, expirados o revocados.
- `empleados`: datos personales y laborales, relacionados 1:1 con `usuarios`.
- `proveedores`: datos comerciales y de contacto.
- `categorias`: jerarquia de categorias y subcategorias.
- `productos`: catalogo, precios, stock y referencias a categoria/proveedor.
- `clientes`: datos de contacto e historial asociado a ventas.
- `ventas`: cabecera de cada venta.
- `detalle_ventas`: lineas de venta con snapshot de nombre y precio.
- `movimientos_stock`: auditoria de entradas, ventas, ajustes, inventario y anulaciones.

Relaciones clave:

```text
usuarios 1--1 empleados
empleados 1--N ventas
clientes 0..1--N ventas
ventas 1--N detalle_ventas
productos 1--N detalle_ventas
productos 1--N movimientos_stock
proveedores 1--N productos
categorias 1--N productos
categorias 1--N categorias
```

Decisiones importantes del modelo:

- Importes monetarios en `NUMERIC(10,2)`.
- Fechas con hora en `TIMESTAMPTZ`.
- Enums persistidos como `VARCHAR` con `CHECK`.
- `updated_at` gestionado con trigger.
- Busqueda de productos optimizada con `pg_trgm` e indice GIN.
- Numero de venta legible con formato `VTA-YYYY-NNNNN`.
- Movimientos de stock tratados como auditoria de solo insercion.

---

## API REST

La API se expone bajo:

```text
http://localhost:8080/api/v1
```

Rutas principales:

| Recurso | Ruta base | Descripcion |
|---|---|---|
| Autenticacion | `/auth` | Login, refresh token, logout y usuario actual |
| Productos | `/productos` | Catalogo, busqueda, alta, edicion y estado |
| Categorias | `/categorias` | Arbol/listado, alta, edicion, estado y borrado |
| Stock | `/stock` | Niveles, movimientos, entradas, ajustes y minimos/maximos |
| Ventas | `/ventas` | Crear venta, historial y anulacion |
| Clientes | `/clientes` | Busqueda, alta normal, alta rapida POS, edicion y estado |
| Proveedores | `/proveedores` | Listado, resumen, alta, edicion y estado |
| Empleados | `/empleados` | Administracion de empleados y cuentas |

Endpoints destacados:

```text
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me

GET    /api/v1/productos
GET    /api/v1/productos/barcode/{ean}
POST   /api/v1/productos
PUT    /api/v1/productos/{id}
PATCH  /api/v1/productos/{id}/estado

GET    /api/v1/stock
GET    /api/v1/stock/{productoId}/movimientos
POST   /api/v1/stock/{productoId}/entradas
POST   /api/v1/stock/{productoId}/ajustes

POST   /api/v1/ventas
GET    /api/v1/ventas
POST   /api/v1/ventas/{ventaId}/anular
```

---

## Estructura del repositorio

```text
erp-angular-springboot/
|-- backend/
|   |-- pom.xml
|   |-- Dockerfile
|   `-- src/
|       |-- main/java/com/algedro/
|       |   |-- auth/
|       |   |-- categoria/
|       |   |-- cliente/
|       |   |-- common/
|       |   |-- config/
|       |   |-- empleado/
|       |   |-- exception/
|       |   |-- producto/
|       |   |-- proveedor/
|       |   |-- security/
|       |   |-- stock/
|       |   |-- usuario/
|       |   `-- venta/
|       |-- main/resources/db/migration/
|       `-- test/com/algedro/
|
|-- frontend/
|   |-- package.json
|   |-- angular.json
|   |-- Dockerfile
|   |-- nginx.conf
|   `-- src/app/
|       |-- core/
|       `-- features/
|
|-- docker-compose.yml
|-- docker-compose.prod.yml
|-- README.md
`-- README_GITHUB.md
```

---

## Ejecucion local

### Requisitos

- Java 17
- Maven
- Node.js 20 o compatible con Angular 17
- Docker Desktop
- Git

### 1. Levantar PostgreSQL y pgAdmin

```bash
docker compose up -d
```

Servicios:

```text
PostgreSQL: localhost:5432
pgAdmin:    http://localhost:5050
```

Valores por defecto:

```text
POSTGRES_DB=algedro
POSTGRES_USER=algedro_user
POSTGRES_PASSWORD=algedro_password
PGADMIN_DEFAULT_EMAIL=admin@algedro.com
PGADMIN_DEFAULT_PASSWORD=admin
```

### 2. Arrancar backend

```bash
cd backend
mvn spring-boot:run
```

API:

```text
http://localhost:8080/api/v1
```

### 3. Arrancar frontend

```bash
cd frontend
npm ci
npm start
```

Aplicacion:

```text
http://localhost:4200
```

---

## Despliegue con Docker

`docker-compose.prod.yml` levanta PostgreSQL, backend Spring Boot y frontend Angular servido por Nginx.

Crear `.env` en la raiz:

```env
POSTGRES_DB=algedro
POSTGRES_USER=algedro_user
POSTGRES_PASSWORD=una_password_segura
JWT_SECRET=un_secreto_largo_de_al_menos_32_caracteres
JWT_EXPIRATION_MS=28800000
JWT_REFRESH_EXPIRATION_MS=604800000
JWT_MAX_FAILED_ATTEMPTS=5
JWT_LOCK_DURATION_MINUTES=15
SPRING_PROFILES_ACTIVE=prod
FRONTEND_PORT=80
```

Arrancar:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up --build
```

Abrir:

```text
http://localhost
```

Detener:

```bash
docker compose -f docker-compose.prod.yml down
```

Borrar tambien datos:

```bash
docker compose -f docker-compose.prod.yml down -v
```

---

## Credenciales de demo

El seed de Flyway (`R__seed.sql`) crea:

| Usuario | Contrasena | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `maria` | `admin123` | EMPLEADO |

Antes de exponer el proyecto fuera de local, cambiar secretos, contrasenas y datos de prueba.

---

## Tests

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm test
```

La suite incluye tests unitarios de servicios, tests de controllers con MockMvc y pruebas de integracion. Las pruebas con Testcontainers requieren Docker; en entornos sin Docker pueden omitirse.

<img width="735" height="569" alt="Tests" src="https://github.com/user-attachments/assets/e612964f-b1ed-4e52-9e0d-03c3c3d5b62f" />

---

## Decisiones de arquitectura

- `usuarios` y `empleados` separados para no mezclar seguridad con datos laborales.
- `detalle_ventas` guarda snapshot de nombre y precio para preservar historico.
- `movimientos_stock` funciona como auditoria de solo insercion.
- Enums como `VARCHAR` + `CHECK` para evolucionar con Flyway.
- Dinero en `NUMERIC(10,2)` y Java `BigDecimal`.
- Fechas con hora en `TIMESTAMPTZ`.
- Busqueda POS con `pg_trgm` e indice GIN.
- Numero de venta legible `VTA-YYYY-NNNNN`, generado por trigger.

---

## Seguridad

Ya contemplado:

- bcrypt para contrasenas.
- JWT firmado.
- Refresh tokens persistidos y revocables.
- Blacklist de access tokens al cerrar sesion.
- Bloqueo por intentos fallidos.
- Rutas protegidas por Spring Security.
- Guards en Angular.

Antes de produccion real:

- Usar un `JWT_SECRET` largo y privado.
- No reutilizar credenciales demo.
- Revisar la persistencia de sesion del frontend: actualmente usa `localStorage`.
- Servir siempre bajo HTTPS.
- Configurar backups de PostgreSQL.
- Evitar datos reales en seeds.
- Decidir si `R__seed.sql` debe ejecutarse solo en demo/desarrollo.