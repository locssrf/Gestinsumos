package modelo;

/** Categoria a la que pertenece un insumo (GES-24). Ej: Lacteos, Limpieza, Medicamentos. */
public class Categoria {

    private int id;
    private String nombre;
    private String descripcion;

    public Categoria() {
    }

    public Categoria(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return String.format("%-4d %-20s %s", id, nombre,
                descripcion == null ? "" : descripcion);
    }
}
