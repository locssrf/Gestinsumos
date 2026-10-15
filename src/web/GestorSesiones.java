package web;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda las sesiones activas (en memoria). Cada navegador recibe un token aleatorio
 * en una cookie; con ese token se encuentra su Sesion.
 * ConcurrentHashMap porque varias peticiones llegan al mismo tiempo (hilos).
 */
public class GestorSesiones {

    public static final String NOMBRE_COOKIE = "SESION_INSUMOS";
    private static final long MINUTOS_INACTIVIDAD = 30;

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();

    public Sesion crear() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sesion s = new Sesion(token);
        sesiones.put(token, s);
        return s;
    }

    /** Devuelve la sesion del token, o null si no existe o expiro por inactividad. */
    public Sesion obtener(String token) {
        limpiarExpiradas();
        if (token == null) return null;
        Sesion s = sesiones.get(token);
        if (s != null) s.tocar();
        return s;
    }

    public void eliminar(String token) {
        if (token != null) sesiones.remove(token);
    }

    public String cookie(Sesion s) {
        return NOMBRE_COOKIE + "=" + s.getToken() + "; Path=/; HttpOnly; SameSite=Strict; Max-Age="
                + (MINUTOS_INACTIVIDAD * 60);
    }

    public String cookieBorrada() {
        return NOMBRE_COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0";
    }

    private void limpiarExpiradas() {
        sesiones.values().removeIf(s -> s.expiro(MINUTOS_INACTIVIDAD));
    }
}
