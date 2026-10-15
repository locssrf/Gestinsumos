package web;

import java.util.Collection;
import java.util.Map;

/**
 * Construye texto JSON sin librerias externas.
 * Ejemplo: Json.objeto().put("nombre", "Leche").put("cantidad", 5).toString()
 *          -> {"nombre":"Leche","cantidad":5}
 */
public class Json {

    private final StringBuilder sb = new StringBuilder("{");
    private boolean vacio = true;

    public static Json objeto() {
        return new Json();
    }

    public Json put(String clave, Object valor) {
        if (!vacio) sb.append(',');
        sb.append(texto(clave)).append(':').append(valor(valor));
        vacio = false;
        return this;   // permite encadenar .put().put().put()
    }

    @Override
    public String toString() {
        return sb + "}";
    }

    /** Convierte cualquier valor soportado a su representacion JSON. */
    public static String valor(Object v) {
        if (v == null) return "null";
        if (v instanceof Json) return v.toString();
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (v instanceof Collection) {
            StringBuilder a = new StringBuilder("[");
            boolean primero = true;
            for (Object o : (Collection<?>) v) {
                if (!primero) a.append(',');
                a.append(valor(o));
                primero = false;
            }
            return a.append(']').toString();
        }
        if (v instanceof Map) {
            Json j = objeto();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) j.put(String.valueOf(e.getKey()), e.getValue());
            return j.toString();
        }
        return texto(v.toString());
    }

    /** Pone comillas y escapa caracteres especiales (comillas, saltos de linea, etc.). */
    public static String texto(String s) {
        StringBuilder r = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': r.append("\\\""); break;
                case '\\': r.append("\\\\"); break;
                case '\n': r.append("\\n"); break;
                case '\r': r.append("\\r"); break;
                case '\t': r.append("\\t"); break;
                default:
                    if (c < 0x20 || c == '<' || c == '>') r.append(String.format("\\u%04x", (int) c));
                    else r.append(c);
            }
        }
        return r.append('"').toString();
    }
}
