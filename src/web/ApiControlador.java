package web;

import com.sun.net.httpserver.HttpExchange;
import modelo.Categoria;
import modelo.EstadoInsumo;
import modelo.Insumo;
import modelo.Rol;
import modelo.UnidadMedida;
import modelo.Usuario;
import servicio.AuthService;
import servicio.InsumoService;
import util.ValidacionException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recibe las peticiones a /api/... y llama a los mismos servicios que usa la consola.
 * Responde siempre en JSON. Es el equivalente web de MenuPrincipal + MenuInsumos.
 *
 * Cada ruta se registra con una expresion lambda que implementa la interfaz Accion.
 */
public class ApiControlador {

    /** Interfaz funcional: lo que hace una ruta. Devuelve el JSON de respuesta. */
    @FunctionalInterface
    interface Accion {
        String ejecutar(Peticion p, Sesion s) throws ValidacionException;
    }

    /** Una ruta registrada: su accion y si exige haber iniciado sesion. */
    private static class Ruta {
        final Accion accion;
        final boolean requiereLogin;

        Ruta(Accion accion, boolean requiereLogin) {
            this.accion = accion;
            this.requiereLogin = requiereLogin;
        }
    }

    private static final int MAX_INTENTOS = 5;
    private static final long MINUTOS_BLOQUEO = 5;

    private final Map<String, Ruta> rutas = new HashMap<>();
    private final GestorSesiones sesiones = new GestorSesiones();
    /** Intentos fallidos de login por usuario+IP (proteccion contra fuerza bruta). */
    private final Map<String, long[]> intentosFallidos = new ConcurrentHashMap<>();

    public ApiControlador() {
        registrarRutas();
    }

    // =====================================================================
    //  REGISTRO DE RUTAS
    // =====================================================================

    private void registrarRutas() {
        // ---------- Sprint 1: autenticacion (publicas) ----------
        publica("GET /api/sesion", (p, s) -> s != null && s.getAuth().haySesionActiva()
                ? Json.objeto().put("autenticado", true).put("usuario", usuarioJson(s.getAuth().getUsuarioActual())).toString()
                : Json.objeto().put("autenticado", false).toString());
        // login y logout se manejan aparte porque crean/borran la cookie
        publica("POST /api/recuperar/pregunta", (p, s) -> Json.objeto()
                .put("pregunta", new AuthService().obtenerPreguntaSeguridad(p.get("username"))).toString());
        publica("POST /api/recuperar", (p, s) -> {
            new AuthService().recuperarPassword(p.get("username"), p.get("respuesta"),
                    p.get("nueva"), p.get("confirmacion"));
            return ok("Contraseña actualizada. Ya puede iniciar sesión.");
        });

        // ---------- Sprint 1: perfil (GES-14) ----------
        privada("GET /api/perfil", (p, s) -> usuarioJson(s.getAuth().getUsuarioActual()).toString());
        privada("POST /api/perfil", (p, s) -> {
            s.getUsuarios().actualizarPerfil(p.get("nombre"), p.get("email"), p.get("telefono"));
            return ok("Perfil actualizado.");
        });
        privada("POST /api/perfil/password", (p, s) -> {
            s.getUsuarios().cambiarPassword(p.get("actual"), p.get("nueva"), p.get("confirmacion"));
            return ok("Contraseña cambiada.");
        });
        privada("POST /api/perfil/pregunta", (p, s) -> {
            s.getUsuarios().cambiarPreguntaSeguridad(p.get("pregunta"), p.get("respuesta"));
            return ok("Pregunta de seguridad actualizada.");
        });

        // ---------- Sprint 1: usuarios y roles (GES-22) ----------
        privada("GET /api/usuarios", (p, s) -> {
            List<Json> lista = new ArrayList<>();
            for (Usuario u : s.getUsuarios().listarUsuarios()) lista.add(usuarioJson(u));
            return Json.valor(lista);
        });
        privada("POST /api/usuarios", (p, s) -> {
            s.getUsuarios().registrarUsuario(p.get("username"), p.get("password"), p.get("nombre"),
                    leerRol(p.get("rol")), p.get("pregunta"), p.get("respuesta"));
            return ok("Usuario registrado.");
        });
        privada("POST /api/usuarios/rol", (p, s) -> {
            s.getUsuarios().cambiarRol(p.getEntero("id"), leerRol(p.get("rol")));
            return ok("Rol actualizado.");
        });
        privada("POST /api/usuarios/estado", (p, s) -> {
            boolean activo = "true".equals(p.get("activo"));
            s.getUsuarios().cambiarEstado(p.getEntero("id"), activo);
            return ok(activo ? "Cuenta activada." : "Cuenta desactivada.");
        });

        // ---------- Sprint 2: categorias (GES-24) ----------
        privada("GET /api/categorias", (p, s) -> {
            List<Json> lista = new ArrayList<>();
            for (Categoria c : s.getCategorias().listar()) {
                lista.add(Json.objeto().put("id", c.getId()).put("nombre", c.getNombre())
                        .put("descripcion", c.getDescripcion() == null ? "" : c.getDescripcion())
                        .put("insumos", s.getCategorias().contarInsumos(c.getId())));
            }
            return Json.valor(lista);
        });
        privada("POST /api/categorias", (p, s) -> {
            s.getCategorias().crear(p.get("nombre"), p.get("descripcion"));
            return ok("Categoría creada.");
        });
        privada("POST /api/categorias/modificar", (p, s) -> {
            s.getCategorias().modificar(p.getEntero("id"), p.get("nombre"), p.get("descripcion"));
            return ok("Categoría actualizada.");
        });
        privada("POST /api/categorias/eliminar", (p, s) -> {
            s.getCategorias().eliminar(p.getEntero("id"));
            return ok("Categoría eliminada.");
        });

        // ---------- Sprint 2: insumos (GES-4, GES-25) y busqueda (GES-3) ----------
        privada("GET /api/unidades", (p, s) -> {
            List<Json> lista = new ArrayList<>();
            for (UnidadMedida u : UnidadMedida.values()) {
                lista.add(Json.objeto().put("codigo", u.name()).put("simbolo", u.getSimbolo())
                        .put("decimales", u.permiteDecimales()));
            }
            return Json.valor(lista);
        });
        privada("GET /api/insumos", this::buscarInsumos);
        privada("POST /api/insumos", (p, s) -> {
            Insumo i = s.getInsumos().registrar(p.get("nombre"), p.get("descripcion"), p.get("categoria"),
                    p.get("cantidad"), p.get("unidad"), p.get("fecha"));
            return Json.objeto().put("mensaje", "Insumo registrado. " + i.textoDias() + ".")
                    .put("insumo", insumoJson(i, s.getInsumos().getDiasAviso())).toString();
        });
        privada("POST /api/insumos/modificar", (p, s) -> {
            Insumo i = s.getInsumos().modificar(p.getEntero("id"), p.get("nombre"), p.get("descripcion"),
                    p.get("categoria"), p.get("cantidad"), p.get("unidad"), p.get("fecha"));
            return Json.objeto().put("mensaje", "Insumo actualizado.")
                    .put("insumo", insumoJson(i, s.getInsumos().getDiasAviso())).toString();
        });
        privada("POST /api/insumos/eliminar", (p, s) -> {
            s.getInsumos().eliminar(p.getEntero("id"));
            return ok("Insumo eliminado.");
        });

        // ---------- Sprint 2: caducidad (GES-2) ----------
        privada("GET /api/alertas", (p, s) -> {
            InsumoService is = s.getInsumos();
            int dias = is.getDiasAviso();
            return Json.objeto().put("diasAviso", dias)
                    .put("caducados", insumosJson(is.listarCaducados(), dias))
                    .put("porCaducar", insumosJson(is.listarPorCaducar(), dias)).toString();
        });
        privada("GET /api/resumen", (p, s) -> {
            InsumoService is = s.getInsumos();
            int dias = is.getDiasAviso();
            int vigentes = 0, porCaducar = 0, caducados = 0;
            List<Insumo> todos = is.listar();
            for (Insumo i : todos) {
                EstadoInsumo e = i.getEstado(dias);
                if (e == EstadoInsumo.VIGENTE) vigentes++;
                else if (e == EstadoInsumo.POR_CADUCAR) porCaducar++;
                else caducados++;
            }
            return Json.objeto().put("total", todos.size()).put("vigentes", vigentes)
                    .put("porCaducar", porCaducar).put("caducados", caducados)
                    .put("categorias", s.getCategorias().listar().size())
                    .put("diasAviso", dias).toString();
        });
        privada("POST /api/caducidad/fecha", (p, s) -> {
            s.getInsumos().actualizarFechaCaducidad(p.getEntero("id"), p.get("fecha"));
            return ok("Fecha de caducidad actualizada.");
        });
        privada("POST /api/caducidad/dias", (p, s) -> {
            s.getInsumos().setDiasAviso(p.getEntero("dias"));
            return ok("Ahora se avisará con " + p.get("dias") + " días de anticipación.");
        });
        privada("POST /api/caducidad/eliminar-caducados", (p, s) -> {
            int n = s.getInsumos().eliminarCaducados();
            return ok("Se eliminaron " + n + " insumo(s) caducados.");
        });
    }

    private void publica(String clave, Accion a) { rutas.put(clave, new Ruta(a, false)); }
    private void privada(String clave, Accion a) { rutas.put(clave, new Ruta(a, true)); }

    // =====================================================================
    //  ATENCION DE PETICIONES
    // =====================================================================

    public void manejar(HttpExchange ex) throws IOException {
        int codigo = 200;
        String respuesta;
        try {
            Peticion p = new Peticion(ex);
            String clave = p.getMetodo() + " " + p.getRuta();
            Sesion s = sesiones.obtener(p.getCookie(GestorSesiones.NOMBRE_COOKIE));

            if (clave.equals("POST /api/login")) {
                respuesta = login(p);
            } else if (clave.equals("POST /api/logout")) {
                respuesta = logout(p, s);
            } else {
                Ruta ruta = rutas.get(clave);
                if (ruta == null) {
                    codigo = 404;
                    respuesta = error("Ruta no encontrada: " + clave);
                } else if (ruta.requiereLogin && !sesionValida(s)) {
                    codigo = 401;
                    respuesta = error("Debe iniciar sesión.");
                } else {
                    respuesta = ruta.accion.ejecutar(p, s);
                }
            }
        } catch (ValidacionException e) {
            codigo = 400;
            respuesta = error(e.getMessage());
        } catch (Exception e) {
            codigo = 500;
            respuesta = error("Error interno del servidor.");
            System.err.println("Error en " + ex.getRequestURI() + ": " + e);
        }
        enviar(ex, codigo, respuesta);
    }

    /**
     * Revisa que la sesion siga valida y recarga el usuario desde la BD.
     * Asi, si el admin desactiva una cuenta o le cambia el rol, se aplica de inmediato.
     */
    private boolean sesionValida(Sesion s) {
        if (s == null || !s.getAuth().haySesionActiva()) return false;
        s.getAuth().refrescarUsuarioActual();
        Usuario u = s.getAuth().getUsuarioActual();
        if (u == null || !u.isActivo()) {
            s.getAuth().cerrarSesion();
            sesiones.eliminar(s.getToken());
            return false;
        }
        return true;
    }

    // ---------- GES-21 / GES-7 ----------

    private String login(Peticion p) throws ValidacionException {
        String clave = p.get("username").trim().toLowerCase() + "|" + p.getIpCliente();
        long[] info = intentosFallidos.get(clave);   // [cantidad, momentoDelUltimoFallo]
        if (info != null && info[0] >= MAX_INTENTOS) {
            long restante = MINUTOS_BLOQUEO * 60_000 - (System.currentTimeMillis() - info[1]);
            if (restante > 0) {
                throw new ValidacionException("Demasiados intentos fallidos. Intente de nuevo en "
                        + (restante / 60_000 + 1) + " minuto(s) o recupere su contraseña.");
            }
            intentosFallidos.remove(clave);
        }

        Sesion s = sesiones.crear();
        try {
            s.getAuth().iniciarSesion(p.get("username"), p.get("password"));
        } catch (ValidacionException e) {
            sesiones.eliminar(s.getToken());
            intentosFallidos.merge(clave, new long[]{1, System.currentTimeMillis()},
                    (a, b) -> new long[]{a[0] + 1, System.currentTimeMillis()});
            throw e;
        }
        intentosFallidos.remove(clave);
        p.agregarCookie(sesiones.cookie(s));
        return Json.objeto().put("mensaje", "Bienvenido, " + s.getAuth().getUsuarioActual().getNombre())
                .put("usuario", usuarioJson(s.getAuth().getUsuarioActual())).toString();
    }

    private String logout(Peticion p, Sesion s) {
        if (s != null) {
            s.getAuth().cerrarSesion();
            sesiones.eliminar(s.getToken());
        }
        p.agregarCookie(sesiones.cookieBorrada());
        return ok("Sesión cerrada.");
    }

    // ---------- GES-3 ----------

    private String buscarInsumos(Peticion p, Sesion s) throws ValidacionException {
        InsumoService is = s.getInsumos();
        List<Insumo> lista;
        String nombre = p.get("nombre").trim();
        String categoria = p.get("categoria").trim();
        String estado = p.get("estado").trim();

        if (!nombre.isEmpty()) lista = is.buscarPorNombre(nombre);
        else if (!categoria.isEmpty()) lista = is.buscarPorCategoria(p.getEntero("categoria"));
        else lista = is.listar();

        // Los filtros se pueden combinar: nombre + categoria + estado
        int dias = is.getDiasAviso();
        List<Insumo> filtrada = new ArrayList<>();
        for (Insumo i : lista) {
            if (!categoria.isEmpty() && !String.valueOf(i.getCategoria().getId()).equals(categoria)) continue;
            if (!estado.isEmpty() && !i.getEstado(dias).name().equals(estado)) continue;
            filtrada.add(i);
        }
        return Json.valor(insumosJson(filtrada, dias));
    }

    // =====================================================================
    //  CONVERSION A JSON
    // =====================================================================

    private Json usuarioJson(Usuario u) {
        return Json.objeto()
                .put("id", u.getId())
                .put("username", u.getUsername())
                .put("nombre", u.getNombre())
                .put("email", u.getEmail() == null ? "" : u.getEmail())
                .put("telefono", u.getTelefono() == null ? "" : u.getTelefono())
                .put("rol", u.getRol().name())
                .put("rolDescripcion", u.getRol().getDescripcion())
                .put("pregunta", u.getPreguntaSeguridad())
                .put("activo", u.isActivo())
                .put("permisos", Json.objeto()
                        .put("gestionarUsuarios", u.puedeGestionarUsuarios())
                        .put("modificarInsumos", u.puedeModificarInsumos()));
    }

    private Json insumoJson(Insumo i, int diasAviso) {
        EstadoInsumo e = i.getEstado(diasAviso);
        return Json.objeto()
                .put("id", i.getId())
                .put("nombre", i.getNombre())
                .put("descripcion", i.getDescripcion() == null ? "" : i.getDescripcion())
                .put("categoriaId", i.getCategoria().getId())
                .put("categoria", i.getCategoria().getNombre())
                .put("cantidad", i.getCantidad())
                .put("unidad", i.getUnidad().name())
                .put("unidadSimbolo", i.getUnidad().getSimbolo())
                .put("fecha", i.getFechaCaducidad().toString())
                .put("fechaTexto", i.getFechaFormateada())
                .put("dias", i.diasParaCaducar())
                .put("tiempo", i.textoDias())
                .put("estado", e.name())
                .put("estadoTexto", e.getDescripcion());
    }

    private List<Json> insumosJson(List<Insumo> lista, int diasAviso) {
        List<Json> r = new ArrayList<>();
        for (Insumo i : lista) r.add(insumoJson(i, diasAviso));
        return r;
    }

    private Rol leerRol(String texto) throws ValidacionException {
        try {
            return Rol.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("Rol no válido.");
        }
    }

    private static String ok(String mensaje) {
        return Json.objeto().put("mensaje", mensaje).toString();
    }

    private static String error(String mensaje) {
        return Json.objeto().put("error", mensaje).toString();
    }

    private void enviar(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(cuerpo);
        }
        ex.close();
    }
}
