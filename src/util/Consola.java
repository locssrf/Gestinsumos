package util;

import java.util.Scanner;

/** Metodos de apoyo para leer datos desde la consola sin repetir codigo. */
public class Consola {

    private static final Scanner SC = new Scanner(System.in);

    private Consola() {
    }

    public static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return SC.nextLine().trim();
    }

    public static int leerEntero(String mensaje) {
        while (true) {
            String entrada = leerTexto(mensaje);
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("  Debe ingresar un número válido.");
            }
        }
    }

    public static boolean confirmar(String mensaje) {
        String r = leerTexto(mensaje + " (s/n): ");
        return r.equalsIgnoreCase("s");
    }

    public static void pausa() {
        leerTexto("\nPresione ENTER para continuar...");
    }

    public static void titulo(String texto) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("  " + texto);
        System.out.println("==================================================");
    }
}
