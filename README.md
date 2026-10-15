# Sistema de Gestión de Insumos

Proyecto de POO en Java + SQLite. Registra insumos con su fecha de caducidad y **avisa cuando están por caducar o ya caducaron**.

Tiene dos formas de usarse, que comparten **las mismas clases** de modelo, DAO y servicio:

- **Versión web** (`app.MainWeb`): página que se abre en el navegador. Es la que se sube a AWS.
- **Versión de consola** (`app.Main`): menús en la terminal.

## Historias de usuario

### Sprint 1 — Gestión de usuarios
| Historia | Qué hace | Dónde está |
|---|---|---|
| GES-21 | Iniciar sesión. En web, bloqueo de 5 min tras 5 intentos fallidos | `AuthService.iniciarSesion()`, `ApiControlador.login()` |
| GES-22 | Roles Administrador / Empleado: cada uno ve y puede hacer cosas distintas | `Usuario`, `Administrador`, `Empleado`, `UsuarioService` |
| GES-23 | Recuperar contraseña con pregunta de seguridad | `AuthService.recuperarPassword()` |
| GES-7  | Cerrar sesión | `AuthService.cerrarSesion()` |
| GES-14 | Editar perfil, contraseña y pregunta de seguridad | `UsuarioService` |

### Sprint 2 — Gestión de insumos
| Historia | Qué hace | Dónde está |
|---|---|---|
| GES-24 | Categorías: crear, editar, eliminar (no se borra si tiene insumos, sin nombres repetidos) | `Categoria`, `CategoriaDAO`, `CategoriaService` |
| GES-4  | Registrar, modificar y eliminar insumos | `InsumoDAO`, `InsumoService` |
| GES-25 | Validación de datos obligatorios: unidad de una lista cerrada, cantidad > 0 (entera para unidad/caja/paquete), fecha válida y no pasada | `InsumoService.validarYConstruir()`, `UnidadMedida` |
| GES-2  | Alertas de caducidad al entrar, días de aviso configurables, actualizar fecha, eliminar caducados | `Insumo.getEstado()`, `InsumoService`, `ConfiguracionDAO` |
| GES-3  | Búsqueda por nombre, categoría y estado (combinables en la web) | `InsumoService.buscarPor...()`, `InsumoDAO` |

## Estructura

```
GestionInsumos/
├─ src/
│  ├─ app/        Main (consola), MainWeb (web)
│  ├─ modelo/     Usuario (abstracta), Administrador, Empleado, Rol,
│  │              Insumo, Categoria, UnidadMedida, EstadoInsumo
│  ├─ db/         ConexionDB  → crea las tablas y datos de ejemplo
│  ├─ dao/        UsuarioDAO, CategoriaDAO, InsumoDAO, ConfiguracionDAO  → SQL
│  ├─ servicio/   AuthService, UsuarioService, CategoriaService, InsumoService → reglas
│  ├─ vista/      MenuPrincipal, MenuInsumos  → interfaz de consola
│  ├─ web/        ServidorWeb, ApiControlador, Sesion, GestorSesiones,
│  │              Peticion, Json, ArchivosEstaticos  → interfaz web
│  └─ util/       Seguridad (SHA-256), Validaciones, Consola, ValidacionException
├─ web/           index.html, css/estilos.css, js/app.js  → la página
├─ lib/           sqlite-jdbc.jar
├─ dist/          GestionInsumos.jar  → listo para ejecutar / subir a AWS
├─ deploy/        gestion-insumos.service (systemd)
├─ compilar.bat / compilar.sh
├─ Dockerfile
└─ DESPLIEGUE_AWS.md
```

### Cómo funciona la versión web

```
Navegador ──HTTP──► ServidorWeb ─┬─ /api/... ─► ApiControlador ─► servicios ─► DAO ─► SQLite
                                 └─ /...     ─► ArchivosEstaticos (HTML, CSS, JS)
```

- Usa el servidor HTTP que ya trae Java (`com.sun.net.httpserver`), sin frameworks.
- Cada navegador recibe una cookie con un token aleatorio. Ese token apunta a un objeto `Sesion` que tiene **su propio** `AuthService`, `InsumoService`, etc. Así se reutilizan los mismos servicios de la consola.
- Toda validación y control de permisos ocurre en el servidor; la página solo muestra u oculta botones.
- Las rutas de la API se registran con **expresiones lambda** que implementan la interfaz funcional `Accion`.

## Conceptos de POO aplicados

| Concepto | Dónde |
|---|---|
| Encapsulamiento | atributos `private` + getters/setters en todos los modelos |
| Abstracción | `Usuario` es abstracta con métodos abstractos |
| Herencia | `Administrador` y `Empleado` extienden `Usuario` |
| Polimorfismo | `puedeModificarInsumos()` / `puedeGestionarUsuarios()` deciden permisos y qué se muestra |
| Composición | `Insumo` tiene un objeto `Categoria`; `Sesion` tiene sus servicios |
| Enums con atributos | `Rol`, `UnidadMedida` (sabe si acepta decimales), `EstadoInsumo` |
| Interfaces y lambdas | `ApiControlador.Accion` |
| Excepción propia | `ValidacionException` |
| Colecciones | `List`, `Map`, `ConcurrentHashMap` (sesiones de varios usuarios a la vez) |
| Fechas | `LocalDate`, `ChronoUnit.DAYS`, `DateTimeFormatter` estricto |

## Seguridad
- Contraseñas guardadas con hash SHA-256 (nunca en texto plano).
- `PreparedStatement` en todas las consultas (evita inyección SQL).
- Cookie de sesión `HttpOnly` y `SameSite=Strict`; la sesión expira tras 30 min sin uso.
- Bloqueo temporal tras 5 intentos fallidos de inicio de sesión.
- Si el administrador desactiva una cuenta o cambia un rol, se aplica en la siguiente acción del usuario.
- La página escapa todo el texto que viene del usuario (evita XSS).

## Cómo ejecutarlo

### En NetBeans
1. Proyecto Java con fuentes existentes → carpeta `src`. Agregar `lib/sqlite-jdbc.jar` en **Libraries**.
2. La carpeta `web/` debe estar en la **raíz del proyecto** (al lado de `src`).
3. Properties → Run → Main Class:
   - `app.MainWeb` → abrir **http://localhost:8080** en el navegador
   - `app.Main` → versión de consola
4. Properties → Sources → Encoding: **UTF-8**.

### Con el .jar
```
java -jar dist/GestionInsumos.jar
```
y abrir http://localhost:8080. Para recompilar el .jar: `compilar.bat` (Windows) o `./compilar.sh`.

### En AWS
Ver **[DESPLIEGUE_AWS.md](DESPLIEGUE_AWS.md)**.

## Datos iniciales
- Usuario: `admin` / `admin123` (pregunta: "Nombre del sistema", respuesta: `insumos`).
- La primera vez se cargan 3 categorías y 5 insumos de ejemplo (uno caducado, dos por caducar, dos vigentes) para mostrar las alertas.

## Base de datos (SQLite)
| Tabla | Contenido |
|---|---|
| `usuarios` | cuentas, rol, contraseña cifrada, pregunta de seguridad, activo |
| `categorias` | nombre único, descripción |
| `insumos` | nombre, cantidad, unidad, fecha de caducidad (`AAAA-MM-DD`), llave foránea a `categorias` |
| `configuracion` | días de aviso de caducidad (por defecto 7) |
