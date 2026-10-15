package servicio;

import dao.CategoriaDAO;
import modelo.Categoria;
import modelo.Usuario;
import util.ValidacionException;
import util.Validaciones;

import java.util.List;

/** GES-24: crear y gestionar categorias de insumos. */
public class CategoriaService {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final AuthService auth;

    public CategoriaService(AuthService auth) {
        this.auth = auth;
    }

    /** Cualquier usuario puede ver las categorias (se usan en la busqueda). */
    public List<Categoria> listar() {
        return categoriaDAO.listar();
    }

    public Categoria buscarPorId(int id) throws ValidacionException {
        Categoria c = categoriaDAO.buscarPorId(id);
        if (c == null) {
            throw new ValidacionException("No existe una categoría con ID " + id + ".");
        }
        return c;
    }

    public int contarInsumos(int idCategoria) {
        return categoriaDAO.contarInsumos(idCategoria);
    }

    public void crear(String nombre, String descripcion) throws ValidacionException {
        verificarPermiso();
        validarNombre(nombre, 0);
        Categoria c = new Categoria(0, nombre.trim(), descripcion == null ? "" : descripcion.trim());
        if (!categoriaDAO.insertar(c)) {
            throw new ValidacionException("No se pudo registrar la categoría.");
        }
    }

    public void modificar(int id, String nombre, String descripcion) throws ValidacionException {
        verificarPermiso();
        Categoria c = buscarPorId(id);
        validarNombre(nombre, id);
        c.setNombre(nombre.trim());
        c.setDescripcion(descripcion == null ? "" : descripcion.trim());
        categoriaDAO.actualizar(c);
    }

    /** No se permite borrar una categoria que todavia tiene insumos (integridad de datos). */
    public void eliminar(int id) throws ValidacionException {
        verificarPermiso();
        Categoria c = buscarPorId(id);
        int cantidad = categoriaDAO.contarInsumos(id);
        if (cantidad > 0) {
            throw new ValidacionException("No se puede eliminar '" + c.getNombre() + "': tiene "
                    + cantidad + " insumo(s) asociados. Muévalos a otra categoría primero.");
        }
        categoriaDAO.eliminar(id);
    }

    // ---------- Validaciones ----------

    private void validarNombre(String nombre, int idActual) throws ValidacionException {
        if (Validaciones.estaVacio(nombre)) {
            throw new ValidacionException("El nombre de la categoría es obligatorio.");
        }
        if (nombre.trim().length() < 3 || nombre.trim().length() > 30) {
            throw new ValidacionException("El nombre debe tener entre 3 y 30 caracteres.");
        }
        Categoria existente = categoriaDAO.buscarPorNombre(nombre);
        if (existente != null && existente.getId() != idActual) {
            throw new ValidacionException("Ya existe una categoría llamada '" + existente.getNombre() + "'.");
        }
    }

    private void verificarPermiso() throws ValidacionException {
        Usuario u = auth.getUsuarioActual();
        if (u == null || !u.puedeModificarInsumos()) {
            throw new ValidacionException("Acceso denegado: solo un administrador puede gestionar categorías.");
        }
    }
}
