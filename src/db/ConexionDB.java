package db;

import util.Seguridad;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Maneja la conexion con SQLite y crea las tablas la primera vez.
 * El archivo "gestion_insumos.db" se crea solo en la carpeta del proyecto.
 */
public class ConexionDB {

    /**
     * Ruta del archivo de la base de datos. Por defecto "gestion_insumos.db" en la carpeta
     * desde donde se ejecuta. En el servidor se puede cambiar con la variable de entorno DB_PATH.
     */
    private static final String RUTA_DB = System.getenv().getOrDefault("DB_PATH", "gestion_insumos.db");
    private static final String URL = "jdbc:sqlite:" + RUTA_DB;

    private ConexionDB() {
    }

    public static Connection conectar() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        // SQLite no revisa las llaves foraneas si no se activa en cada conexion
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            // En la version web varias personas usan la BD a la vez: si esta ocupada, espera hasta 5 s
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return con;
    }

    /** Crea las tablas si no existen y un administrador por defecto. */
    public static void inicializar() {
        crearCarpetaDeLaBaseDeDatos();
        String tablaUsuarios =
                "CREATE TABLE IF NOT EXISTS usuarios ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password TEXT NOT NULL,"
                + " nombre TEXT NOT NULL,"
                + " email TEXT,"
                + " telefono TEXT,"
                + " rol TEXT NOT NULL CHECK (rol IN ('ADMIN','EMPLEADO')),"
                + " pregunta_seguridad TEXT NOT NULL,"
                + " respuesta_seguridad TEXT NOT NULL,"
                + " activo INTEGER NOT NULL DEFAULT 1"
                + ")";

        // Tablas del Sprint 2: se dejan creadas desde ya
        String tablaCategorias =
                "CREATE TABLE IF NOT EXISTS categorias ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                + " descripcion TEXT"
                + ")";

        String tablaInsumos =
                "CREATE TABLE IF NOT EXISTS insumos ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " nombre TEXT NOT NULL,"
                + " descripcion TEXT,"
                + " categoria_id INTEGER,"
                + " cantidad REAL NOT NULL CHECK (cantidad >= 0),"
                + " unidad TEXT NOT NULL,"
                + " fecha_caducidad TEXT NOT NULL,"   // formato AAAA-MM-DD
                + " FOREIGN KEY (categoria_id) REFERENCES categorias(id)"
                + ")";

        // Configuracion general del sistema (ej: dias de aviso antes de caducar)
        String tablaConfiguracion =
                "CREATE TABLE IF NOT EXISTS configuracion ("
                + " clave TEXT PRIMARY KEY,"
                + " valor TEXT NOT NULL"
                + ")";

        try (Connection con = conectar(); Statement st = con.createStatement()) {
            st.execute(tablaUsuarios);
            st.execute(tablaCategorias);
            st.execute(tablaInsumos);
            st.execute(tablaConfiguracion);
            st.execute("INSERT OR IGNORE INTO configuracion (clave, valor) VALUES ('dias_aviso', '7')");
            crearAdminPorDefecto(con);
            cargarDatosDeEjemplo(con);
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    /** Si DB_PATH apunta a una carpeta que no existe (ej: /opt/app/datos/), la crea. */
    private static void crearCarpetaDeLaBaseDeDatos() {
        java.io.File carpeta = new java.io.File(RUTA_DB).getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            System.out.println("No se pudo crear la carpeta " + carpeta);
        }
    }

    private static void crearAdminPorDefecto(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuarios")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // ya hay usuarios
            }
        }

        String sql = "INSERT INTO usuarios (username, password, nombre, email, telefono, rol, "
                + "pregunta_seguridad, respuesta_seguridad, activo) VALUES (?,?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, Seguridad.hash("admin123"));
            ps.setString(3, "Administrador General");
            ps.setString(4, "admin@insumos.com");
            ps.setString(5, "");
            ps.setString(6, "ADMIN");
            ps.setString(7, "Nombre del sistema");
            ps.setString(8, Seguridad.hashRespuesta("insumos"));
            ps.executeUpdate();
            System.out.println("Se creó el usuario por defecto -> usuario: admin | contraseña: admin123");
        }
    }

    /**
     * Carga categorias e insumos de ejemplo UNA sola vez (para poder mostrar las alertas
     * en la presentacion). Las fechas se calculan desde hoy, asi siempre hay insumos
     * caducados, por caducar y vigentes.
     */
    private static void cargarDatosDeEjemplo(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT valor FROM configuracion WHERE clave = 'datos_ejemplo'")) {
            if (rs.next()) {
                return; // ya se cargaron antes
            }
        }

        try (Statement st = con.createStatement()) {
            st.execute("INSERT OR IGNORE INTO categorias (nombre, descripcion) VALUES "
                    + "('Lácteos', 'Leche, quesos, yogures'),"
                    + "('Limpieza', 'Productos de aseo'),"
                    + "('Medicamentos', 'Botiquín y primeros auxilios')");
        }

        LocalDate hoy = LocalDate.now();
        Object[][] insumos = {
                {"Leche entera", "Lácteos", 12, "L", hoy.minusDays(2)},
                {"Yogur natural", "Lácteos", 8, "UNIDAD", hoy.plusDays(3)},
                {"Queso campesino", "Lácteos", 2.5, "KG", hoy.plusDays(6)},
                {"Detergente", "Limpieza", 5, "KG", hoy.plusDays(200)},
                {"Acetaminofén 500mg", "Medicamentos", 3, "CAJA", hoy.plusDays(90)},
        };

        String sql = "INSERT INTO insumos (nombre, descripcion, categoria_id, cantidad, unidad, fecha_caducidad) "
                + "VALUES (?, '', (SELECT id FROM categorias WHERE nombre = ?), ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Object[] fila : insumos) {
                ps.setString(1, (String) fila[0]);
                ps.setString(2, (String) fila[1]);
                ps.setDouble(3, ((Number) fila[2]).doubleValue());
                ps.setString(4, (String) fila[3]);
                ps.setString(5, fila[4].toString());   // formato AAAA-MM-DD
                ps.executeUpdate();
            }
        }

        try (Statement st = con.createStatement()) {
            st.execute("INSERT INTO configuracion (clave, valor) VALUES ('datos_ejemplo', '1')");
        }
        System.out.println("Se cargaron categorías e insumos de ejemplo.");
    }
}
