package vista;

import modelo.Rol;
import modelo.Usuario;
import servicio.AuthService;
import servicio.UsuarioService;
import util.Consola;
import util.ValidacionException;

import java.util.List;

/**
 * Interfaz de consola del sistema.
 * Solo muestra menus y lee datos: toda la logica esta en los servicios.
 */
public class MenuPrincipal {

    private static final int MAX_INTENTOS = 3;

    private final AuthService auth = new AuthService();
    private final UsuarioService usuarioService = new UsuarioService(auth);
    private final MenuInsumos menuInsumos = new MenuInsumos(auth);

    public void iniciar() {
        int opcion;
        do {
            Consola.titulo("SISTEMA DE GESTIÓN DE INSUMOS");
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Olvidé mi contraseña");
            System.out.println("0. Salir");
            opcion = Consola.leerEntero("Seleccione una opción: ");

            switch (opcion) {
                case 1:
                    iniciarSesion();
                    break;
                case 2:
                    recuperarPassword();
                    break;
                case 0:
                    System.out.println("Hasta pronto.");
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        } while (opcion != 0);
    }

    // ================== GES-21: INICIAR SESION ==================

    private void iniciarSesion() {
        Consola.titulo("INICIAR SESIÓN");
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            String username = Consola.leerTexto("Usuario: ");
            String password = Consola.leerTexto("Contraseña: ");
            try {
                auth.iniciarSesion(username, password);
                System.out.println("\nBienvenido, " + auth.getUsuarioActual().getNombre()
                        + " (" + auth.getUsuarioActual().getRol().getDescripcion() + ")");
                // GES-2: al entrar, el sistema avisa si hay insumos caducados o por caducar
                menuInsumos.mostrarAlertas(false);
                menuUsuario();
                return;
            } catch (ValidacionException e) {
                System.out.println("  " + e.getMessage()
                        + " Intentos restantes: " + (MAX_INTENTOS - intento));
            }
        }
        System.out.println("Demasiados intentos fallidos. Use la opción 'Olvidé mi contraseña'.");
    }

    // ================== GES-23: RECUPERAR CONTRASENA ==================

    private void recuperarPassword() {
        Consola.titulo("RECUPERAR CONTRASEÑA");
        String username = Consola.leerTexto("Ingrese su usuario: ");
        try {
            String pregunta = auth.obtenerPreguntaSeguridad(username);
            System.out.println("Pregunta de seguridad: " + pregunta);
            String respuesta = Consola.leerTexto("Respuesta: ");
            String nueva = Consola.leerTexto("Nueva contraseña: ");
            String confirmacion = Consola.leerTexto("Confirme la contraseña: ");
            auth.recuperarPassword(username, respuesta, nueva, confirmacion);
            System.out.println("Contraseña actualizada. Ya puede iniciar sesión.");
        } catch (ValidacionException e) {
            System.out.println("  " + e.getMessage());
        }
        Consola.pausa();
    }

    // ================== MENU DESPUES DEL LOGIN ==================

    private void menuUsuario() {
        while (auth.haySesionActiva()) {
            Usuario u = auth.getUsuarioActual();
            Consola.titulo("MENU PRINCIPAL - " + u.getUsername() + " [" + u.getRol().getDescripcion() + "]");
            System.out.println("1. Ver mi perfil");
            System.out.println("2. Editar mi perfil");
            System.out.println("3. Cambiar contraseña");
            System.out.println("4. Cambiar pregunta de seguridad");
            System.out.println("5. Modulo de insumos");
            // Polimorfismo: la opcion solo aparece si el objeto es Administrador
            if (u.puedeGestionarUsuarios()) {
                System.out.println("6. Gestión de usuarios y roles");
            }
            System.out.println("0. Cerrar sesión");

            int opcion = Consola.leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1: verPerfil(); break;
                case 2: editarPerfil(); break;
                case 3: cambiarPassword(); break;
                case 4: cambiarPregunta(); break;
                case 5:
                    menuInsumos.mostrar();
                    break;
                case 6:
                    if (u.puedeGestionarUsuarios()) {
                        menuAdministracion();
                    } else {
                        System.out.println("Opción no válida.");
                    }
                    break;
                case 0:
                    cerrarSesion();
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    // ================== GES-7: CERRAR SESION ==================

    private void cerrarSesion() {
        if (Consola.confirmar("¿Seguro que desea cerrar sesión?")) {
            auth.cerrarSesion();
            System.out.println("Sesión cerrada correctamente.");
        }
    }

    // ================== GES-14: PERFIL ==================

    private void verPerfil() {
        Usuario u = auth.getUsuarioActual();
        Consola.titulo("MI PERFIL");
        System.out.println("Usuario   : " + u.getUsername());
        System.out.println("Nombre    : " + u.getNombre());
        System.out.println("Correo    : " + valorOVacio(u.getEmail()));
        System.out.println("Teléfono  : " + valorOVacio(u.getTelefono()));
        System.out.println("Rol       : " + u.getRol().getDescripcion());
        System.out.println("Pregunta  : " + u.getPreguntaSeguridad());
        Consola.pausa();
    }

    private void editarPerfil() {
        Usuario u = auth.getUsuarioActual();
        Consola.titulo("EDITAR PERFIL");
        System.out.println("(Deje en blanco para conservar el valor actual)");

        String nombre = pedirConValorActual("Nombre", u.getNombre());
        String email = pedirConValorActual("Correo", u.getEmail());
        String telefono = pedirConValorActual("Teléfono", u.getTelefono());

        try {
            usuarioService.actualizarPerfil(nombre, email, telefono);
            System.out.println("Perfil actualizado correctamente.");
        } catch (ValidacionException e) {
            System.out.println("  " + e.getMessage());
        }
        Consola.pausa();
    }

    private void cambiarPassword() {
        Consola.titulo("CAMBIAR CONTRASEÑA");
        String actual = Consola.leerTexto("Contraseña actual: ");
        String nueva = Consola.leerTexto("Nueva contraseña: ");
        String confirmacion = Consola.leerTexto("Confirme la nueva contraseña: ");
        try {
            usuarioService.cambiarPassword(actual, nueva, confirmacion);
            System.out.println("Contraseña cambiada correctamente.");
        } catch (ValidacionException e) {
            System.out.println("  " + e.getMessage());
        }
        Consola.pausa();
    }

    private void cambiarPregunta() {
        Consola.titulo("CAMBIAR PREGUNTA DE SEGURIDAD");
        String pregunta = Consola.leerTexto("Nueva pregunta: ");
        String respuesta = Consola.leerTexto("Respuesta: ");
        try {
            usuarioService.cambiarPreguntaSeguridad(pregunta, respuesta);
            System.out.println("Pregunta de seguridad actualizada.");
        } catch (ValidacionException e) {
            System.out.println("  " + e.getMessage());
        }
        Consola.pausa();
    }

    // ================== GES-22: ROLES Y PERMISOS ==================

    private void menuAdministracion() {
        int opcion;
        do {
            Consola.titulo("GESTIÓN DE USUARIOS Y ROLES");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Registrar nuevo usuario");
            System.out.println("3. Cambiar rol de un usuario");
            System.out.println("4. Activar / desactivar usuario");
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Seleccione una opción: ");

            try {
                switch (opcion) {
                    case 1: listarUsuarios(); break;
                    case 2: registrarUsuario(); break;
                    case 3: cambiarRol(); break;
                    case 4: cambiarEstado(); break;
                    case 0: break;
                    default: System.out.println("Opción no válida.");
                }
            } catch (ValidacionException e) {
                System.out.println("  " + e.getMessage());
                Consola.pausa();
            }
        } while (opcion != 0);
    }

    private void listarUsuarios() throws ValidacionException {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        Consola.titulo("LISTA DE USUARIOS");
        System.out.println(String.format("%-4s %-15s %-25s %-15s %s", "ID", "USUARIO", "NOMBRE", "ROL", "ESTADO"));
        System.out.println("--------------------------------------------------------------------------");
        for (Usuario u : usuarios) {
            System.out.println(u);
        }
        Consola.pausa();
    }

    private void registrarUsuario() throws ValidacionException {
        Consola.titulo("REGISTRAR USUARIO");
        String username = Consola.leerTexto("Nombre de usuario: ");
        String password = Consola.leerTexto("Contraseña: ");
        String nombre = Consola.leerTexto("Nombre completo: ");
        Rol rol = pedirRol();
        String pregunta = Consola.leerTexto("Pregunta de seguridad: ");
        String respuesta = Consola.leerTexto("Respuesta: ");

        usuarioService.registrarUsuario(username, password, nombre, rol, pregunta, respuesta);
        System.out.println("Usuario registrado correctamente.");
        Consola.pausa();
    }

    private void cambiarRol() throws ValidacionException {
        int id = Consola.leerEntero("ID del usuario: ");
        Rol rol = pedirRol();
        usuarioService.cambiarRol(id, rol);
        System.out.println("Rol actualizado a " + rol.getDescripcion() + ".");
        Consola.pausa();
    }

    private void cambiarEstado() throws ValidacionException {
        int id = Consola.leerEntero("ID del usuario: ");
        boolean activar = Consola.confirmar("¿Desea ACTIVAR la cuenta? (n = desactivar)");
        usuarioService.cambiarEstado(id, activar);
        System.out.println("Cuenta " + (activar ? "activada." : "desactivada."));
        Consola.pausa();
    }

    // ================== METODOS DE APOYO ==================

    private Rol pedirRol() {
        while (true) {
            System.out.println("Roles: 1. Administrador  2. Empleado");
            int op = Consola.leerEntero("Seleccione el rol: ");
            if (op == 1) return Rol.ADMIN;
            if (op == 2) return Rol.EMPLEADO;
            System.out.println("Rol no válido.");
        }
    }

    private String pedirConValorActual(String campo, String actual) {
        String entrada = Consola.leerTexto(campo + " [" + valorOVacio(actual) + "]: ");
        return entrada.isEmpty() ? actual : entrada;
    }

    private String valorOVacio(String valor) {
        return (valor == null || valor.isEmpty()) ? "(sin definir)" : valor;
    }
}
