package app;

import db.ConexionDB;
import vista.MenuPrincipal;

import java.util.TimeZone;

/**
 * Punto de entrada de la VERSION DE CONSOLA del Sistema de Gestion de Insumos.
 * Sprint 1: Gestion de usuarios | Sprint 2: Gestion de insumos.
 */
public class Main {

    /** Zona horaria de Colombia: asi "hoy" es el mismo dia aunque el servidor este en otro pais. */
    public static final String ZONA_HORARIA = "America/Bogota";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        ConexionDB.inicializar();
        new MenuPrincipal().iniciar();
    }
}
