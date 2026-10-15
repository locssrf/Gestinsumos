package web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Entrega los archivos de la pagina (HTML, CSS, JS, imagenes).
 * Primero los busca en la carpeta "web/" junto al programa (asi el compañero de estilos
 * puede editar el CSS sin recompilar). Si no existe, los toma de adentro del .jar (carpeta "publico").
 */
public class ArchivosEstaticos {

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "svg", "image/svg+xml",
            "ico", "image/x-icon",
            "json", "application/json; charset=utf-8");

    private final Path carpeta;

    public ArchivosEstaticos() {
        carpeta = Paths.get(System.getenv().getOrDefault("WEB_DIR", "web")).toAbsolutePath().normalize();
    }

    public void manejar(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                enviar(ex, 405, "text/plain", "Método no permitido".getBytes());
                return;
            }
            String ruta = ex.getRequestURI().getPath();
            if (ruta.equals("/") || ruta.isEmpty()) ruta = "/index.html";

            byte[] contenido = leer(ruta);
            if (contenido == null) {
                enviar(ex, 404, "text/plain; charset=utf-8", "Página no encontrada".getBytes());
                return;
            }
            String ext = ruta.substring(ruta.lastIndexOf('.') + 1).toLowerCase();
            enviar(ex, 200, TIPOS.getOrDefault(ext, "application/octet-stream"), contenido);
        } finally {
            ex.close();
        }
    }

    private byte[] leer(String ruta) throws IOException {
        // Seguridad: no permitir salir de la carpeta con "../"
        Path archivo = carpeta.resolve(ruta.substring(1)).normalize();
        if (!archivo.startsWith(carpeta)) return null;
        if (Files.isRegularFile(archivo)) return Files.readAllBytes(archivo);

        if (ruta.contains("..")) return null;
        try (InputStream in = ArchivosEstaticos.class.getResourceAsStream("/publico" + ruta)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private void enviar(HttpExchange ex, int codigo, String tipo, byte[] cuerpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(cuerpo);
        }
    }
}
