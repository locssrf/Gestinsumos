# Gestinsumos

**Gestinsumos** es un sistema de gestión de inventario diseñado para automatizar el control de vencimiento de insumos y productos mediante un método de **semaforización** (verde, amarillo, rojo) que clasifica automáticamente cada ítem según su proximidad a la fecha de caducidad.

## Objetivo

Reducir al mínimo las pérdidas económicas y operativas causadas por productos vencidos, dando visibilidad en tiempo real del estado del inventario y anticipando su consumo antes de que caduque.

## Características principales

- **Semaforización automática**: clasifica los insumos por colores según umbrales de días configurables, adaptables a distintos tipos de producto.
- **Dashboard de métricas**: visualiza mermas evitadas, insumos próximos a vencer y estadísticas clave del inventario.
- **Alertas automáticas por correo**: notifica al equipo cuando un insumo entra en estado crítico (rojo).
- **Gestión de insumos**: creación, edición y categorización de productos, con validación de datos obligatorios (unidad de medida, cantidad, fecha).
- **Historial de movimientos**: trazabilidad completa de quién registró, modificó o consumió cada insumo.
- **Gestión de usuarios y roles**: control de acceso diferenciado entre administradores y usuarios.
- **Reportes exportables**: generación de reportes en PDF y Excel para auditorías.
- **Búsqueda y filtrado**: localización rápida de insumos por nombre o categoría.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | _Por definir_ |
| Frontend | _Por definir_ |
| Base de datos | _Por definir_ |
| Autenticación | _Por definir_ |
| Notificaciones | _Por definir_ |

## Estructura del proyecto

```
gestinsumos/
├── backend/          # API y lógica de negocio
├── frontend/         # Interfaz de usuario
├── docs/             # Documentación adicional
└── README.md
```

## Instalación

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/gestinsumos.git
cd gestinsumos

# Instalar dependencias
# (completa según tu stack: npm install / pip install -r requirements.txt / etc.)

# Configurar variables de entorno
cp .env.example .env

# Ejecutar el proyecto
# (completa el comando de arranque)
```

## Requisitos previos

- _Por definir (ej. Node.js, Python, Docker, etc.)_

## Roadmap

El desarrollo está organizado en sprints bajo metodología Scrum:

1. **Sprint 1 — Gestión de usuario**: autenticación, roles y permisos.
2. **Sprint 2 — Gestión de insumos**: CRUD, categorías y validaciones.
3. **Sprint 3 — Control y movimiento**: semaforización, historial y alertas.
4. **Sprint 4 — Reportes y consulta**: dashboard y exportación de reportes.

## Licencia

_Por definir_

## Estado del proyecto

En desarrollo activo.
