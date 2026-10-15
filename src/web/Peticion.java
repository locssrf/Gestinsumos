package web;

import com.sun.net.httpserver.HttpExchange;
import util.ValidacionException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Envuelve una peticion HTTP y facilita leer sus datos.
 * Los formularios llegan como "clave=valor&clave2=valor2" (x-www-form-urlencoded).
 */
public class Peticion {

    private static final int TAMANO_MAXIMO = 64 * 1024;   // 64 KB es mas que suficiente

    private final HttpExchange intercambio;
    private final Map<String, String> parametros = new HashMap<>();

    public Peticion(HttpExchange intercambio) throws IOException {
        this.intercambio = intercambio;
        leerParametros(intercambio.getRequestURI().getRawQuery());
        if ("POST".equals(getMetodo())) {
            leerParametros(leerCuerpo(intercambio.getRequestBody()));
        }
    }

    public String getMetodo() {
        return intercambio.getRequestMethod().toUpperCase();
    }

    public String getRuta() {
        String ruta = intercambio.getRequestURI().getPath();
        return ruta.endsWith("/") && ruta.length() > 1 ? ruta.substring(0, ruta.length() - 1) : ruta;
    }

    /** Devuelve el parametro o "" si no vino. */
    public String get(String clave) {
        return parametros.getOrDefault(clave, "");
    }

    public int getEntero(String clave) throws ValidacionException {
        try {
            return Integer.parseInt(get(clave).trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo '" + clave + "' debe ser un número.");
        }
    }

    public String getCookie(String nombre) {
        String header = intercambio.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String parte : header.split(";")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(nombre)) return kv[1];
        }
        return null;
    }

    public void agregarCookie(String valor) {
        intercambio.getResponseHeaders().add("Set-Cookie", valor);
    }

    public String getIpCliente() {
        return intercambio.getRemoteAddress().getAddress().getHostAddress();
    }

    // ---------- privados ----------

    private void leerParametros(String texto) {
        if (texto == null || texto.isEmpty()) return;
        for (String par : texto.split("&")) {
            String[] kv = par.split("=", 2);
            String clave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            parametros.put(clave, valor);
        }
    }

    private static String leerCuerpo(InputStream in) throws IOException {
        byte[] datos = in.readNBytes(TAMANO_MAXIMO + 1);
        if (datos.length > TAMANO_MAXIMO) {
            throw new IOException("Petición demasiado grande");
        }
        return new String(datos, StandardCharsets.UTF_8);
    }
}
