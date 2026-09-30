package db;

import util.Seguridad;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Maneja la conexion con SQLite y crea las tablas la primera vez.
 * El archivo "gestion_insumos.db" se crea solo en la carpeta del proyecto.
 */
public class ConexionDB {

    private static final String URL = "jdbc:sqlite:gestion_insumos.db";

    private ConexionDB() {
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Crea las tablas si no existen y un administrador por defecto. */
    public static void inicializar() {
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
                + " nombre TEXT NOT NULL UNIQUE,"
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

        try (Connection con = conectar(); Statement st = con.createStatement()) {
            st.execute(tablaUsuarios);
            st.execute(tablaCategorias);
            st.execute(tablaInsumos);
            crearAdminPorDefecto(con);
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
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
            System.out.println("Se creo el usuario por defecto -> usuario: admin | contrasena: admin123");
        }
    }
}
