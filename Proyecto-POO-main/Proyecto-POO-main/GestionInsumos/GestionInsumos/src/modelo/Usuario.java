package modelo;

/**
 * Clase ABSTRACTA que representa a cualquier usuario del sistema.
 * No se puede instanciar directamente: siempre se crea un Administrador o un Empleado.
 *
 * Conceptos de POO aplicados:
 *  - Encapsulamiento: atributos privados con getters y setters.
 *  - Abstraccion: metodos abstractos que cada subclase implementa a su manera.
 *  - Herencia / Polimorfismo: Administrador y Empleado heredan de Usuario.
 */
public abstract class Usuario {

    private int id;
    private String username;
    private String passwordHash;        // nunca guardamos la contrasena en texto plano
    private String nombre;
    private String email;
    private String telefono;
    private String preguntaSeguridad;
    private String respuestaSeguridadHash;
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(String username, String passwordHash, String nombre) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
    }

    // ---------- Metodos abstractos (polimorfismo) ----------

    /** Cada tipo de usuario devuelve su propio rol. */
    public abstract Rol getRol();

    /** Permiso para crear usuarios, cambiar roles, activar/desactivar cuentas. */
    public abstract boolean puedeGestionarUsuarios();

    /** Permiso para crear, modificar o eliminar insumos (Sprint 2). */
    public abstract boolean puedeModificarInsumos();

    /** Todos los usuarios pueden consultar/buscar insumos (GES-3). */
    public boolean puedeConsultarInsumos() {
        return true;
    }

    // ---------- Metodo "fabrica" ----------

    /** Crea el objeto correcto segun el rol que viene de la base de datos. */
    public static Usuario crearSegunRol(Rol rol) {
        if (rol == Rol.ADMIN) {
            return new Administrador();
        }
        return new Empleado();
    }

    // ---------- Getters y Setters ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPreguntaSeguridad() { return preguntaSeguridad; }
    public void setPreguntaSeguridad(String preguntaSeguridad) { this.preguntaSeguridad = preguntaSeguridad; }

    public String getRespuestaSeguridadHash() { return respuestaSeguridadHash; }
    public void setRespuestaSeguridadHash(String respuestaSeguridadHash) { this.respuestaSeguridadHash = respuestaSeguridadHash; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return String.format("%-4d %-15s %-25s %-15s %s",
                id, username, nombre, getRol().getDescripcion(), activo ? "Activo" : "Inactivo");
    }
}
