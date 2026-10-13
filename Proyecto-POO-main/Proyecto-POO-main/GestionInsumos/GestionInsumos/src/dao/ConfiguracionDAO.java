package dao;

import db.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Lee y guarda valores de configuracion (por ahora: dias de aviso de caducidad). */
public class ConfiguracionDAO {

    private static final int DIAS_AVISO_POR_DEFECTO = 7;

    public int getDiasAviso() {
        String sql = "SELECT valor FROM configuracion WHERE clave = 'dias_aviso'";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Integer.parseInt(rs.getString("valor"));
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Error al leer configuracion: " + e.getMessage());
        }
        return DIAS_AVISO_POR_DEFECTO;
    }

    public boolean setDiasAviso(int dias) {
        String sql = "INSERT OR REPLACE INTO configuracion (clave, valor) VALUES ('dias_aviso', ?)";
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(dias));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al guardar configuracion: " + e.getMessage());
            return false;
        }
    }
}
