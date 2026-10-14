# Sistema de Gestión de Insumos — Avance Sprint 1

Proyecto de POO en Java con base de datos SQLite.

## Avance entregado (~30 %)

| Historia | Descripción | Dónde está |
|---|---|---|
| GES-21 | Iniciar sesión con credenciales (máx. 3 intentos) | `AuthService.iniciarSesion()` |
| GES-22 | Roles y permisos (Administrador / Empleado) | `Usuario`, `Administrador`, `Empleado`, `UsuarioService` |
| GES-23 | Recuperar contraseña con pregunta de seguridad | `AuthService.recuperarPassword()` |
| GES-7  | Cerrar sesión | `AuthService.cerrarSesion()` |
| GES-14 | Personalizar perfil (nombre, correo, teléfono, contraseña, pregunta) | `UsuarioService` |
| Base Sprint 2 | Tablas `insumos` y `categorias` + clase `Insumo` con cálculo de días para caducar | `ConexionDB`, `modelo/Insumo.java` |

## Estructura (arquitectura por capas)

```
src/
 ├─ app/        Main.java              → punto de entrada
 ├─ modelo/     Usuario (abstracta), Administrador, Empleado, Rol (enum), Insumo
 ├─ db/         ConexionDB             → conexión SQLite y creación de tablas
 ├─ dao/        UsuarioDAO             → consultas SQL (PreparedStatement)
 ├─ servicio/   AuthService, UsuarioService → reglas de negocio y validaciones
 ├─ vista/      MenuPrincipal          → menús de consola
 └─ util/       Seguridad (SHA-256), Validaciones, Consola, ValidacionException
lib/
 └─ sqlite-jdbc.jar                    → driver de SQLite
```

## Conceptos de POO aplicados

- **Encapsulamiento:** atributos `private` con getters/setters.
- **Abstracción:** `Usuario` es clase abstracta con métodos abstractos.
- **Herencia:** `Administrador` y `Empleado` extienden `Usuario`.
- **Polimorfismo:** `puedeGestionarUsuarios()` responde distinto según el objeto; el menú muestra u oculta opciones según eso.
- **Enum:** `Rol`.
- **Excepción propia:** `ValidacionException`.

## Cómo ejecutarlo

**NetBeans / IntelliJ / Eclipse:** crear proyecto Java, copiar las carpetas de `src`, agregar `lib/sqlite-jdbc.jar` como librería y ejecutar `app.Main`.

**Consola:**
```
javac -d out -cp lib/sqlite-jdbc.jar src/*/*.java
java -cp "out;lib/sqlite-jdbc.jar" app.Main      (Windows)
java -cp "out:lib/sqlite-jdbc.jar" app.Main      (Linux/Mac)
```

La base de datos `gestion_insumos.db` se crea sola la primera vez.

**Usuario inicial:** `admin` / `admin123` (pregunta de seguridad: "Nombre del sistema", respuesta: `insumos`).
