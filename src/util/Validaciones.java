package util;

public class Validaciones {

    private Validaciones() {
    }

    public static boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public static boolean passwordValida(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean emailValido(String email) {
        return email != null && email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    }

    public static boolean telefonoValido(String telefono) {
        return telefono != null && telefono.matches("^[0-9]{7,10}$");
    }

    public static boolean usernameValido(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{4,20}$");
    }
}
