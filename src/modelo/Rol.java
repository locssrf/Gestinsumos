package modelo;

/**
 * Roles del sistema (GES-22).
 * Un enum sirve para limitar los valores posibles: solo existen estos dos roles.
 */
public enum Rol {
    ADMIN("Administrador"),
    EMPLEADO("Empleado");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
