package servicio;

import dao.CategoriaDAO;
import dao.ConfiguracionDAO;
import dao.InsumoDAO;
import modelo.Categoria;
import modelo.EstadoInsumo;
import modelo.Insumo;
import modelo.UnidadMedida;
import modelo.Usuario;
import util.ValidacionException;
import util.Validaciones;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica de insumos:
 *  - GES-4  Anadir, modificar y eliminar insumos
 *  - GES-25 Validar datos obligatorios (unidad, cantidad, fecha)
 *  - GES-2  Gestionar fechas de caducidad y alertas
 *  - GES-3  Buscar insumos
 */
public class InsumoService {

    /** STRICT + "uuuu" hace que fechas imposibles como 31/02/2026 sean rechazadas. */
    private static final DateTimeFormatter FORMATO_ESTRICTO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_ISO_ESTRICTO =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private static final double CANTIDAD_MAXIMA = 100000;
    private static final int DIAS_AVISO_MAXIMO = 365;

    private final InsumoDAO insumoDAO = new InsumoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ConfiguracionDAO configDAO = new ConfiguracionDAO();
    private final AuthService auth;

    public InsumoService(AuthService auth) {
        this.auth = auth;
    }

    // ================== CRUD (GES-4) ==================

    /** Recibe los datos como texto (como los escribe el usuario) y los valida antes de guardar. */
    public Insumo registrar(String nombre, String descripcion, String idCategoriaTxt,
                            String cantidadTxt, String unidadTxt, String fechaTxt) throws ValidacionException {
        verificarPermiso();
        Insumo i = validarYConstruir(nombre, descripcion, idCategoriaTxt, cantidadTxt, unidadTxt, fechaTxt);

        // Regla: no tiene sentido registrar un insumo que ya llego caducado
        if (i.estaCaducado()) {
            throw new ValidacionException("No se puede registrar un insumo con fecha de caducidad pasada.");
        }
        if (!insumoDAO.insertar(i)) {
            throw new ValidacionException("No se pudo registrar el insumo.");
        }
        return i;
    }

    public Insumo modificar(int id, String nombre, String descripcion, String idCategoriaTxt,
                            String cantidadTxt, String unidadTxt, String fechaTxt) throws ValidacionException {
        verificarPermiso();
        buscarPorId(id);  // lanza excepcion si no existe
        Insumo i = validarYConstruir(nombre, descripcion, idCategoriaTxt, cantidadTxt, unidadTxt, fechaTxt);
        i.setId(id);
        insumoDAO.actualizar(i);
        return i;
    }

    public void eliminar(int id) throws ValidacionException {
        verificarPermiso();
        buscarPorId(id);
        insumoDAO.eliminar(id);
    }

    public Insumo buscarPorId(int id) throws ValidacionException {
        Insumo i = insumoDAO.buscarPorId(id);
        if (i == null) {
            throw new ValidacionException("No existe un insumo con ID " + id + ".");
        }
        return i;
    }

    public List<Insumo> listar() {
        return insumoDAO.listar();
    }

    // ================== VALIDACIONES (GES-25) ==================

    /**
     * Valida TODOS los datos obligatorios. Si algo falla lanza ValidacionException
     * con un mensaje claro y no se guarda nada.
     */
    private Insumo validarYConstruir(String nombre, String descripcion, String idCategoriaTxt,
                                     String cantidadTxt, String unidadTxt, String fechaTxt) throws ValidacionException {
        // 1. Nombre
        if (Validaciones.estaVacio(nombre)) {
            throw new ValidacionException("El nombre del insumo es obligatorio.");
        }
        if (nombre.trim().length() < 2 || nombre.trim().length() > 50) {
            throw new ValidacionException("El nombre debe tener entre 2 y 50 caracteres.");
        }

        // 2. Categoria
        Categoria categoria = validarCategoria(idCategoriaTxt);

        // 3. Unidad de medida
        if (Validaciones.estaVacio(unidadTxt)) {
            throw new ValidacionException("La unidad de medida es obligatoria.");
        }
        UnidadMedida unidad = UnidadMedida.desdeTexto(unidadTxt);
        if (unidad == null) {
            throw new ValidacionException("Unidad '" + unidadTxt + "' no válida. Opciones: " + UnidadMedida.opciones());
        }

        // 4. Cantidad
        double cantidad = validarCantidad(cantidadTxt, unidad);

        // 5. Fecha de caducidad
        LocalDate fecha = convertirFecha(fechaTxt);

        Insumo i = new Insumo();
        i.setNombre(nombre.trim());
        i.setDescripcion(descripcion == null ? "" : descripcion.trim());
        i.setCategoria(categoria);
        i.setUnidad(unidad);
        i.setCantidad(cantidad);
        i.setFechaCaducidad(fecha);
        return i;
    }

    private Categoria validarCategoria(String idTxt) throws ValidacionException {
        if (Validaciones.estaVacio(idTxt)) {
            throw new ValidacionException("La categoría es obligatoria.");
        }
        int id;
        try {
            id = Integer.parseInt(idTxt.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("Debe escribir el ID numérico de la categoría.");
        }
        Categoria c = categoriaDAO.buscarPorId(id);
        if (c == null) {
            throw new ValidacionException("No existe una categoría con ID " + id + ".");
        }
        return c;
    }

    private double validarCantidad(String cantidadTxt, UnidadMedida unidad) throws ValidacionException {
        if (Validaciones.estaVacio(cantidadTxt)) {
            throw new ValidacionException("La cantidad es obligatoria.");
        }
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadTxt.trim().replace(',', '.'));  // acepta 2,5 y 2.5
        } catch (NumberFormatException e) {
            throw new ValidacionException("La cantidad debe ser un número (ej: 10 o 2.5).");
        }
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que cero.");
        }
        if (cantidad > CANTIDAD_MAXIMA) {
            throw new ValidacionException("La cantidad no puede superar " + (long) CANTIDAD_MAXIMA + ".");
        }
        if (!unidad.permiteDecimales() && cantidad != Math.floor(cantidad)) {
            throw new ValidacionException("Para la unidad '" + unidad.getSimbolo()
                    + "' la cantidad debe ser un número entero.");
        }
        return cantidad;
    }

    /**
     * Convierte la fecha a LocalDate. Acepta "dd/mm/aaaa" (consola) y "aaaa-mm-dd" (formulario web).
     * Rechaza formatos incorrectos y fechas imposibles.
     */
    public LocalDate convertirFecha(String fechaTxt) throws ValidacionException {
        if (Validaciones.estaVacio(fechaTxt)) {
            throw new ValidacionException("La fecha de caducidad es obligatoria.");
        }
        LocalDate fecha;
        try {
            String t = fechaTxt.trim();
            fecha = t.contains("-")
                    ? LocalDate.parse(t, FORMATO_ISO_ESTRICTO)
                    : LocalDate.parse(t, FORMATO_ESTRICTO);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("Fecha inválida. Use el formato dd/mm/aaaa (ej: 25/12/2026).");
        }
        if (fecha.isAfter(LocalDate.now().plusYears(10))) {
            throw new ValidacionException("La fecha de caducidad no puede ser a más de 10 años.");
        }
        return fecha;
    }

    // ================== CADUCIDAD (GES-2) ==================

    public int getDiasAviso() {
        return configDAO.getDiasAviso();
    }

    public void setDiasAviso(int dias) throws ValidacionException {
        verificarPermiso();
        if (dias < 1 || dias > DIAS_AVISO_MAXIMO) {
            throw new ValidacionException("Los días de aviso deben estar entre 1 y " + DIAS_AVISO_MAXIMO + ".");
        }
        configDAO.setDiasAviso(dias);
    }

    public List<Insumo> listarCaducados() {
        return insumoDAO.listarCaducados();
    }

    public List<Insumo> listarPorCaducar() {
        return insumoDAO.listarPorCaducar(getDiasAviso());
    }

    /** Cambiar solo la fecha (por ejemplo, llego un lote nuevo del mismo insumo). */
    public void actualizarFechaCaducidad(int id, String fechaTxt) throws ValidacionException {
        verificarPermiso();
        buscarPorId(id);
        LocalDate nueva = convertirFecha(fechaTxt);
        if (nueva.isBefore(LocalDate.now())) {
            throw new ValidacionException("La nueva fecha no puede ser anterior a hoy.");
        }
        insumoDAO.actualizarFechaCaducidad(id, nueva);
    }

    public int eliminarCaducados() throws ValidacionException {
        verificarPermiso();
        return insumoDAO.eliminarCaducados();
    }

    // ================== BUSQUEDA (GES-3) ==================

    public List<Insumo> buscarPorNombre(String texto) throws ValidacionException {
        if (Validaciones.estaVacio(texto)) {
            throw new ValidacionException("Escriba al menos una letra para buscar.");
        }
        return insumoDAO.buscarPorNombre(texto);
    }

    public List<Insumo> buscarPorCategoria(int idCategoria) throws ValidacionException {
        if (categoriaDAO.buscarPorId(idCategoria) == null) {
            throw new ValidacionException("No existe una categoría con ID " + idCategoria + ".");
        }
        return insumoDAO.buscarPorCategoria(idCategoria);
    }

    /** Filtra la lista completa usando el estado calculado en la clase Insumo. */
    public List<Insumo> buscarPorEstado(EstadoInsumo estado) {
        int dias = getDiasAviso();
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo i : insumoDAO.listar()) {
            if (i.getEstado(dias) == estado) {
                resultado.add(i);
            }
        }
        return resultado;
    }

    // ================== PERMISOS ==================

    private void verificarPermiso() throws ValidacionException {
        Usuario u = auth.getUsuarioActual();
        if (u == null || !u.puedeModificarInsumos()) {
            throw new ValidacionException("Acceso denegado: solo un administrador puede modificar insumos.");
        }
    }
}
