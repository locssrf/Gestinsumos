package modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Representa un insumo del inventario.
 *
 * Conceptos de POO:
 *  - Encapsulamiento: atributos privados.
 *  - Composicion: un Insumo TIENE una Categoria (objeto, no solo un texto).
 *  - Usa los enums UnidadMedida y EstadoInsumo.
 */
public class Insumo {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int id;
    private String nombre;
    private String descripcion;
    private Categoria categoria;
    private double cantidad;
    private UnidadMedida unidad;
    private LocalDate fechaCaducidad;

    public Insumo() {
    }

    // ---------- Logica de caducidad (GES-2) ----------

    /** Dias que faltan para caducar (negativo si ya caduco). */
    public long diasParaCaducar() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad);
    }

    public boolean estaCaducado() {
        return diasParaCaducar() < 0;
    }

    /** true si caduca hoy o dentro de los proximos 'diasAviso' dias. */
    public boolean estaPorCaducar(int diasAviso) {
        long dias = diasParaCaducar();
        return dias >= 0 && dias <= diasAviso;
    }

    public EstadoInsumo getEstado(int diasAviso) {
        if (estaCaducado()) return EstadoInsumo.CADUCADO;
        if (estaPorCaducar(diasAviso)) return EstadoInsumo.POR_CADUCAR;
        return EstadoInsumo.VIGENTE;
    }

    /** Texto amigable: "Vence hoy", "Faltan 3 días", "Venció hace 2 días". */
    public String textoDias() {
        long d = diasParaCaducar();
        if (d == 0) return "Vence hoy";
        if (d > 0) return "Faltan " + d + (d == 1 ? " día" : " días");
        return "Venció hace " + (-d) + (d == -1 ? " día" : " días");
    }

    public String getFechaFormateada() {
        return fechaCaducidad.format(FORMATO_FECHA);
    }

    /** Cantidad sin ".0" cuando es un numero entero (5 en vez de 5.0). */
    public String getCantidadFormateada() {
        if (cantidad == Math.floor(cantidad)) {
            return String.valueOf((long) cantidad);
        }
        return String.valueOf(cantidad);
    }

    // ---------- Getters y Setters ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public UnidadMedida getUnidad() { return unidad; }
    public void setUnidad(UnidadMedida unidad) { this.unidad = unidad; }

    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(LocalDate fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }
}
