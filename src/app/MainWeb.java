package app;

import db.ConexionDB;
import web.ServidorWeb;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

/**
 * Punto de entrada de la VERSION WEB.
 * Ejecutar y abrir en el navegador: http://localhost:8080
 * En AWS: http://IP-PUBLICA:8080 (o el puerto definido en la variable de entorno PORT).
 */
public class MainWeb {

    public static void main(String[] args) throws Exception {
        // Los registros (logs) del servidor en UTF-8 para que las tildes se vean bien en Linux/AWS
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        TimeZone.setDefault(TimeZone.getTimeZone(Main.ZONA_HORARIA));
        ConexionDB.inicializar();

        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        ServidorWeb servidor = new ServidorWeb(puerto);
        servidor.iniciar();

        System.out.println("==================================================");
        System.out.println("  Sistema de Gestión de Insumos - versión web");
        System.out.println("  Abra en el navegador: http://localhost:" + puerto);
        System.out.println("  Usuario inicial: admin / admin123");
        System.out.println("  (Ctrl + C o el botón Stop para detenerlo)");
        System.out.println("==================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
    }
}
