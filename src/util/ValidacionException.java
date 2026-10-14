package util;

/**
 * Excepcion propia del sistema. Se lanza cuando los datos no son validos
 * (usuario incorrecto, contrasena corta, sin permisos, etc.).
 */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
