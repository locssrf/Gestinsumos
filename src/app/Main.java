package app;

import db.ConexionDB;
import vista.MenuPrincipal;

/**
 * Punto de entrada del Sistema de Gestion de Insumos.
 * Sprint 1: Gestion de usuarios | Sprint 2: Gestion de insumos.
 */
public class Main {

    public static void main(String[] args) {
        ConexionDB.inicializar();
        new MenuPrincipal().iniciar();
    }
}
