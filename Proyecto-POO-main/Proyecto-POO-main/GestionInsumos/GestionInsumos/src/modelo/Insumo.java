package modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Modelo base de un insumo (preparacion para el Sprint 2).
 * Ya incluye la logica principal del sistema: saber cuanto falta para que caduque.
 */
public class Insumo {

    private int id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private double cantidad;
    private String unidad;           // kg, litros, unidades, etc.
    private LocalDate fechaCaducidad;

    public Insumo() {
    }

    public Insumo(String nombre, String categoria, double cantidad, String unidad, LocalDate fechaCaducidad) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.fechaCaducidad = fechaCaducidad;
    }

    /** Dias que faltan para caducar (negativo si ya caduco). */
    public long diasParaCaducar() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad);
    }

    public boolean estaCaducado() {
        return diasParaCaducar() < 0;
    }

    /** true si caduca dentro de los proximos 'diasAviso' dias. */
    public boolean estaPorCaducar(int diasAviso) {
        long dias = diasParaCaducar();
        return dias >= 0 && dias <= diasAviso;
    }

    public String getEstado(int diasAviso) {
        if (estaCaducado()) return "CADUCADO";
        if (estaPorCaducar(diasAviso)) return "POR CADUCAR";
        return "VIGENTE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(LocalDate fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }

    @Override
    public String toString() {
        return String.format("%-4d %-20s %-15s %8.2f %-8s %s",
                id, nombre, categoria, cantidad, unidad, fechaCaducidad);
    }
}
