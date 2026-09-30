package modelo;

/** El administrador tiene todos los permisos del sistema. */
public class Administrador extends Usuario {

    public Administrador() {
        super();
    }

    public Administrador(String username, String passwordHash, String nombre) {
        super(username, passwordHash, nombre);
    }

    @Override
    public Rol getRol() {
        return Rol.ADMIN;
    }

    @Override
    public boolean puedeGestionarUsuarios() {
        return true;
    }

    @Override
    public boolean puedeModificarInsumos() {
        return true;
    }
}
