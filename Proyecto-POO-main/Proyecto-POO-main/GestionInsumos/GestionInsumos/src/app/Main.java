package app;

import db.ConexionDB;
import vista.MenuPrincipal;

/**
 * Punto de entrada del Sistema de Gestion de Insumos.
 * Avance: Sprint 1 (Gestion de usuarios) + estructura base del Sprint 2.
 */
public class Main {

    public static void main(String[] args) {
        ConexionDB.inicializar();
        new MenuPrincipal().iniciar();
    }
}
