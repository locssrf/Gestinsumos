package servicio;

import dao.UsuarioDAO;
import modelo.Rol;
import modelo.Usuario;
import util.Seguridad;
import util.ValidacionException;
import util.Validaciones;

import java.util.List;

/**
 * Logica de usuarios:
 *  - GES-14 Personalizar perfil (cualquier usuario sobre si mismo)
 *  - GES-22 Gestionar roles y permisos (solo administradores)
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AuthService auth;

    public UsuarioService(AuthService auth) {
        this.auth = auth;
    }

    // ================== PERFIL (GES-14) ==================

    public void actualizarPerfil(String nombre, String email, String telefono) throws ValidacionException {
        Usuario u = auth.getUsuarioActual();

        if (Validaciones.estaVacio(nombre)) {
            throw new ValidacionException("El nombre no puede estar vacio.");
        }
        if (!Validaciones.estaVacio(email) && !Validaciones.emailValido(email)) {
            throw new ValidacionException("El correo no tiene un formato valido.");
        }
        if (!Validaciones.estaVacio(telefono) && !Validaciones.telefonoValido(telefono)) {
            throw new ValidacionException("El telefono debe tener entre 7 y 10 digitos.");
        }

        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono(telefono);
        usuarioDAO.actualizarPerfil(u);
        auth.refrescarUsuarioActual();
    }

    public void cambiarPassword(String actual, String nueva, String confirmacion) throws ValidacionException {
        Usuario u = auth.getUsuarioActual();
        if (!u.getPasswordHash().equals(Seguridad.hash(actual))) {
            throw new ValidacionException("La contrasena actual es incorrecta.");
        }
        if (!Validaciones.passwordValida(nueva)) {
            throw new ValidacionException("La nueva contrasena debe tener al menos 6 caracteres.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ValidacionException("Las contrasenas no coinciden.");
        }
        usuarioDAO.actualizarPassword(u.getId(), Seguridad.hash(nueva));
        auth.refrescarUsuarioActual();
    }

    public void cambiarPreguntaSeguridad(String pregunta, String respuesta) throws ValidacionException {
        if (Validaciones.estaVacio(pregunta) || Validaciones.estaVacio(respuesta)) {
            throw new ValidacionException("La pregunta y la respuesta son obligatorias.");
        }
        Usuario u = auth.getUsuarioActual();
        usuarioDAO.actualizarPreguntaSeguridad(u.getId(), pregunta, Seguridad.hashRespuesta(respuesta));
        auth.refrescarUsuarioActual();
    }

    // ================== ADMINISTRACION (GES-22) ==================

    /** Verifica que el usuario actual tenga permiso. Si no, lanza excepcion. */
    private void verificarPermisoAdmin() throws ValidacionException {
        Usuario u = auth.getUsuarioActual();
        if (u == null || !u.puedeGestionarUsuarios()) {
            throw new ValidacionException("Acceso denegado: solo un administrador puede hacer esto.");
        }
    }

    public List<Usuario> listarUsuarios() throws ValidacionException {
        verificarPermisoAdmin();
        return usuarioDAO.listar();
    }

    public void registrarUsuario(String username, String password, String nombre, Rol rol,
                                 String pregunta, String respuesta) throws ValidacionException {
        verificarPermisoAdmin();

        if (!Validaciones.usernameValido(username)) {
            throw new ValidacionException("El usuario debe tener de 4 a 20 caracteres (letras, numeros o _).");
        }
        if (usuarioDAO.existeUsername(username)) {
            throw new ValidacionException("Ese nombre de usuario ya existe.");
        }
        if (!Validaciones.passwordValida(password)) {
            throw new ValidacionException("La contrasena debe tener al menos 6 caracteres.");
        }
        if (Validaciones.estaVacio(nombre)) {
            throw new ValidacionException("El nombre es obligatorio.");
        }
        if (Validaciones.estaVacio(pregunta) || Validaciones.estaVacio(respuesta)) {
            throw new ValidacionException("La pregunta y respuesta de seguridad son obligatorias.");
        }

        Usuario nuevo = Usuario.crearSegunRol(rol);   // polimorfismo
        nuevo.setUsername(username);
        nuevo.setPasswordHash(Seguridad.hash(password));
        nuevo.setNombre(nombre);
        nuevo.setEmail("");
        nuevo.setTelefono("");
        nuevo.setPreguntaSeguridad(pregunta);
        nuevo.setRespuestaSeguridadHash(Seguridad.hashRespuesta(respuesta));

        if (!usuarioDAO.insertar(nuevo)) {
            throw new ValidacionException("No se pudo registrar el usuario.");
        }
    }

    public void cambiarRol(int idUsuario, Rol nuevoRol) throws ValidacionException {
        verificarPermisoAdmin();
        Usuario objetivo = obtenerOtroUsuario(idUsuario);
        if (objetivo.getRol() == nuevoRol) {
            throw new ValidacionException("El usuario ya tiene ese rol.");
        }
        usuarioDAO.cambiarRol(idUsuario, nuevoRol);
    }

    public void cambiarEstado(int idUsuario, boolean activo) throws ValidacionException {
        verificarPermisoAdmin();
        obtenerOtroUsuario(idUsuario);
        usuarioDAO.cambiarEstado(idUsuario, activo);
    }

    /** Busca un usuario distinto al actual (el admin no puede quitarse permisos a si mismo). */
    private Usuario obtenerOtroUsuario(int idUsuario) throws ValidacionException {
        Usuario objetivo = usuarioDAO.buscarPorId(idUsuario);
        if (objetivo == null) {
            throw new ValidacionException("No existe un usuario con ese ID.");
        }
        if (objetivo.getId() == auth.getUsuarioActual().getId()) {
            throw new ValidacionException("No puede modificar su propio rol o estado.");
        }
        return objetivo;
    }
}
