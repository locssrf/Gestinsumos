package modelo;

/** Estado de un insumo segun su fecha de caducidad (GES-2). */
public enum EstadoInsumo {
    VIGENTE("Vigente"),
    POR_CADUCAR("Por caducar"),
    CADUCADO("CADUCADO");

    private final String descripcion;

    EstadoInsumo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
