package modelo;

/**
 * Unidades de medida permitidas para un insumo (GES-25).
 * Al ser un enum, no se puede registrar una unidad inventada.
 * Algunas unidades (unidad, caja, paquete) solo aceptan cantidades enteras.
 */
public enum UnidadMedida {
    KG("kg", true),
    G("g", true),
    L("L", true),
    ML("ml", true),
    UNIDAD("unidad", false),
    CAJA("caja", false),
    PAQUETE("paquete", false);

    private final String simbolo;
    private final boolean permiteDecimales;

    UnidadMedida(String simbolo, boolean permiteDecimales) {
        this.simbolo = simbolo;
        this.permiteDecimales = permiteDecimales;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public boolean permiteDecimales() {
        return permiteDecimales;
    }

    /** Busca la unidad por su simbolo o su nombre, sin importar mayusculas. Devuelve null si no existe. */
    public static UnidadMedida desdeTexto(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        for (UnidadMedida u : values()) {
            if (u.simbolo.equalsIgnoreCase(t) || u.name().equalsIgnoreCase(t)) {
                return u;
            }
        }
        return null;
    }

    /** Texto con todas las opciones, para mostrar en el menu. */
    public static String opciones() {
        StringBuilder sb = new StringBuilder();
        for (UnidadMedida u : values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(u.simbolo);
        }
        return sb.toString();
    }
}
