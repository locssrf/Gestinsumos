package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utilidades para proteger contrasenas usando SHA-256. */
public class Seguridad {

    private Seguridad() {
        // clase de utilidades: no se instancia
    }

    public static String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /** Las respuestas de seguridad se normalizan para que "Bogota" y " bogota " sean iguales. */
    public static String hashRespuesta(String respuesta) {
        return hash(respuesta.trim().toLowerCase());
    }
}
