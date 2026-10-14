package modelo;

/** El empleado solo puede consultar insumos y editar su propio perfil. */
public class Empleado extends Usuario {

    public Empleado() {
        super();
    }

    public Empleado(String username, String passwordHash, String nombre) {
        super(username, passwordHash, nombre);
    }

    @Override
    public Rol getRol() {
        return Rol.EMPLEADO;
    }

    @Override
    public boolean puedeGestionarUsuarios() {
        return false;
    }

    @Override
    public boolean puedeModificarInsumos() {
        return false;
    }
}
