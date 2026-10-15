package web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP incluido en Java (com.sun.net.httpserver): no necesita Tomcat ni librerias.
 *  - /api/...  -> ApiControlador (datos en JSON)
 *  - /...      -> ArchivosEstaticos (la pagina: HTML, CSS, JS)
 */
public class ServidorWeb {

    private final int puerto;
    private HttpServer servidor;

    public ServidorWeb(int puerto) {
        this.puerto = puerto;
    }

    public void iniciar() throws IOException {
        ApiControlador api = new ApiControlador();
        ArchivosEstaticos estaticos = new ArchivosEstaticos();

        // 0.0.0.0 = escuchar en todas las interfaces (necesario para entrar por la IP publica en AWS)
        servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", puerto), 0);
        servidor.createContext("/api/", api::manejar);
        servidor.createContext("/", estaticos::manejar);
        servidor.setExecutor(Executors.newFixedThreadPool(10));
        servidor.start();
    }

    public void detener() {
        if (servidor != null) servidor.stop(1);
    }
}
