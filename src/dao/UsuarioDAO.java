package dao;

import db.ConexionDB;
import modelo.Rol;
import modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO = Data Access Object.
 * Es la unica clase que escribe SQL sobre la tabla "usuarios".
 * Se usan PreparedStatement para evitar inyeccion SQL.
 */
public class UsuarioDAO {

    public boolean insertar(Usuario u) {
        String sql = "INSERT INTO usuarios (username, password, nombre, email, telefono, rol, "
                + "pregunta_seguridad, respuesta_seguridad, activo) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombre());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getRol().name());
            ps.setString(7, u.getPreguntaSeguridad());
            ps.setString(8, u.getRespuestaSeguridadHash());
            ps.setInt(9, u.isActivo() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarPorUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar usuario: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorId(int id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar usuario: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY id";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizarPerfil(Usuario u) {
        String sql = "UPDATE usuarios SET nombre = ?, email = ?, telefono = ? WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getTelefono());
            ps.setInt(4, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar perfil: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarPassword(int id, String passwordHash) {
        return actualizarCampo("UPDATE usuarios SET password = ? WHERE id = ?", passwordHash, id);
    }

    public boolean actualizarPreguntaSeguridad(int id, String pregunta, String respuestaHash) {
        String sql = "UPDATE usuarios SET pregunta_seguridad = ?, respuesta_seguridad = ? WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pregunta);
            ps.setString(2, respuestaHash);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar pregunta: " + e.getMessage());
            return false;
        }
    }

    public boolean cambiarRol(int id, Rol rol) {
        return actualizarCampo("UPDATE usuarios SET rol = ? WHERE id = ?", rol.name(), id);
    }

    public boolean cambiarEstado(int id, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al cambiar estado: " + e.getMessage());
            return false;
        }
    }

    public boolean existeUsername(String username) {
        return buscarPorUsername(username) != null;
    }

    // ---------- Metodos privados de apoyo ----------

    private boolean actualizarCampo(String sql, String valor, int id) {
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    /** Convierte una fila de la tabla en un objeto Administrador o Empleado. */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Rol rol = Rol.valueOf(rs.getString("rol"));
        Usuario u = Usuario.crearSegunRol(rol);
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password"));
        u.setNombre(rs.getString("nombre"));
        u.setEmail(rs.getString("email"));
        u.setTelefono(rs.getString("telefono"));
        u.setPreguntaSeguridad(rs.getString("pregunta_seguridad"));
        u.setRespuestaSeguridadHash(rs.getString("respuesta_seguridad"));
        u.setActivo(rs.getInt("activo") == 1);
        return u;
    }
}
