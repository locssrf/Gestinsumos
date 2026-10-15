package servicio;

import dao.UsuarioDAO;
import modelo.Usuario;
import util.Seguridad;
import util.ValidacionException;
import util.Validaciones;

/**
 * Logica de autenticacion:
 *  - GES-21 Iniciar sesion
 *  - GES-7  Cerrar sesion
 *  - GES-23 Recuperar contrasena
 */
public class AuthService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario usuarioActual;   // usuario con sesion iniciada (null si no hay)

    public void iniciarSesion(String username, String password) throws ValidacionException {
        if (Validaciones.estaVacio(username) || Validaciones.estaVacio(password)) {
            throw new ValidacionException("Debe ingresar usuario y contraseña.");
        }

        Usuario u = usuarioDAO.buscarPorUsername(username);

        // Mismo mensaje en ambos casos para no revelar si el usuario existe
        if (u == null || !u.getPasswordHash().equals(Seguridad.hash(password))) {
            throw new ValidacionException("Usuario o contraseña incorrectos.");
        }
        if (!u.isActivo()) {
            throw new ValidacionException("Su cuenta está desactivada. Contacte al administrador.");
        }
        usuarioActual = u;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    public boolean haySesionActiva() {
        return usuarioActual != null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /** Vuelve a cargar el usuario desde la BD (por ejemplo, despues de editar el perfil). */
    public void refrescarUsuarioActual() {
        if (usuarioActual != null) {
            usuarioActual = usuarioDAO.buscarPorId(usuarioActual.getId());
        }
    }

    /** Paso 1 de la recuperacion: obtener la pregunta de seguridad. */
    public String obtenerPreguntaSeguridad(String username) throws ValidacionException {
        Usuario u = usuarioDAO.buscarPorUsername(username);
        if (u == null) {
            throw new ValidacionException("No existe un usuario con ese nombre.");
        }
        return u.getPreguntaSeguridad();
    }

    /** Paso 2 de la recuperacion: validar respuesta y guardar la nueva contrasena. */
    public void recuperarPassword(String username, String respuesta,
                                  String nueva, String confirmacion) throws ValidacionException {
        Usuario u = usuarioDAO.buscarPorUsername(username);
        if (u == null) {
            throw new ValidacionException("No existe un usuario con ese nombre.");
        }
        if (!u.getRespuestaSeguridadHash().equals(Seguridad.hashRespuesta(respuesta))) {
            throw new ValidacionException("La respuesta de seguridad es incorrecta.");
        }
        if (!Validaciones.passwordValida(nueva)) {
            throw new ValidacionException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ValidacionException("Las contraseñas no coinciden.");
        }
        usuarioDAO.actualizarPassword(u.getId(), Seguridad.hash(nueva));
    }
}
