package dao;

import db.ConexionDB;
import modelo.Categoria;
import modelo.Insumo;
import modelo.UnidadMedida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla "insumos" (GES-4, GES-2, GES-3).
 * Las fechas se guardan como texto AAAA-MM-DD; en ese formato se pueden
 * comparar con <, > y BETWEEN directamente en SQL.
 */
public class InsumoDAO {

    /** Consulta base: trae el insumo junto con el nombre de su categoria (LEFT JOIN). */
    private static final String SELECT_BASE =
            "SELECT i.*, c.nombre AS cat_nombre, c.descripcion AS cat_descripcion "
            + "FROM insumos i LEFT JOIN categorias c ON i.categoria_id = c.id ";

    // ================== CRUD (GES-4) ==================

    public boolean insertar(Insumo i) {
        String sql = "INSERT INTO insumos (nombre, descripcion, categoria_id, cantidad, unidad, fecha_caducidad) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarParametros(ps, i);
            boolean ok = ps.executeUpdate() > 0;
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (ok && llaves.next()) i.setId(llaves.getInt(1));   // el ID que asigno SQLite
            }
            return ok;
        } catch (SQLException e) {
            System.out.println("Error al registrar insumo: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Insumo i) {
        String sql = "UPDATE insumos SET nombre = ?, descripcion = ?, categoria_id = ?, cantidad = ?, "
                + "unidad = ?, fecha_caducidad = ? WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            llenarParametros(ps, i);
            ps.setInt(7, i.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar insumo: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM insumos WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar insumo: " + e.getMessage());
            return false;
        }
    }

    public Insumo buscarPorId(int id) {
        List<Insumo> r = consultar(SELECT_BASE + "WHERE i.id = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    public List<Insumo> listar() {
        return consultar(SELECT_BASE + "ORDER BY i.fecha_caducidad");
    }

    // ================== BUSQUEDAS (GES-3) ==================

    /** Busca por parte del nombre: "lec" encuentra "Leche entera". */
    public List<Insumo> buscarPorNombre(String texto) {
        return consultar(SELECT_BASE + "WHERE LOWER(i.nombre) LIKE LOWER(?) ORDER BY i.nombre",
                "%" + texto.trim() + "%");
    }

    public List<Insumo> buscarPorCategoria(int idCategoria) {
        return consultar(SELECT_BASE + "WHERE i.categoria_id = ? ORDER BY i.fecha_caducidad", idCategoria);
    }

    // ================== CADUCIDAD (GES-2) ==================

    /** Insumos con fecha anterior a hoy. */
    public List<Insumo> listarCaducados() {
        return consultar(SELECT_BASE + "WHERE i.fecha_caducidad < ? ORDER BY i.fecha_caducidad",
                LocalDate.now().toString());
    }

    /** Insumos que vencen entre hoy y hoy + dias. */
    public List<Insumo> listarPorCaducar(int dias) {
        LocalDate hoy = LocalDate.now();
        return consultar(SELECT_BASE + "WHERE i.fecha_caducidad BETWEEN ? AND ? ORDER BY i.fecha_caducidad",
                hoy.toString(), hoy.plusDays(dias).toString());
    }

    public boolean actualizarFechaCaducidad(int id, LocalDate nuevaFecha) {
        String sql = "UPDATE insumos SET fecha_caducidad = ? WHERE id = ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevaFecha.toString());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar fecha: " + e.getMessage());
            return false;
        }
    }

    /** Borra todos los insumos caducados y devuelve cuantos se borraron. */
    public int eliminarCaducados() {
        String sql = "DELETE FROM insumos WHERE fecha_caducidad < ?";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al eliminar caducados: " + e.getMessage());
            return 0;
        }
    }

    // ================== Metodos privados de apoyo ==================

    private void llenarParametros(PreparedStatement ps, Insumo i) throws SQLException {
        ps.setString(1, i.getNombre());
        ps.setString(2, i.getDescripcion());
        ps.setInt(3, i.getCategoria().getId());
        ps.setDouble(4, i.getCantidad());
        ps.setString(5, i.getUnidad().name());
        ps.setString(6, i.getFechaCaducidad().toString());
    }

    /**
     * Ejecuta un SELECT con parametros y convierte cada fila en un Insumo.
     * "Object... parametros" (varargs) permite pasar 0, 1 o varios valores.
     */
    private List<Insumo> consultar(String sql, Object... parametros) {
        List<Insumo> lista = new ArrayList<>();
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int k = 0; k < parametros.length; k++) {
                ps.setObject(k + 1, parametros[k]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar insumos: " + e.getMessage());
        }
        return lista;
    }

    private Insumo mapear(ResultSet rs) throws SQLException {
        Insumo i = new Insumo();
        i.setId(rs.getInt("id"));
        i.setNombre(rs.getString("nombre"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setCantidad(rs.getDouble("cantidad"));
        i.setUnidad(UnidadMedida.valueOf(rs.getString("unidad")));
        i.setFechaCaducidad(LocalDate.parse(rs.getString("fecha_caducidad")));

        String nombreCat = rs.getString("cat_nombre");
        i.setCategoria(new Categoria(rs.getInt("categoria_id"),
                nombreCat == null ? "Sin categoría" : nombreCat,
                rs.getString("cat_descripcion")));
        return i;
    }
}
