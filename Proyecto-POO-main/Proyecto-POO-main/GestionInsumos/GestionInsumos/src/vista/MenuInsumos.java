package vista;

import modelo.Categoria;
import modelo.EstadoInsumo;
import modelo.Insumo;
import modelo.UnidadMedida;
import modelo.Usuario;
import servicio.AuthService;
import servicio.CategoriaService;
import servicio.InsumoService;
import util.Consola;
import util.ValidacionException;

import java.util.List;

/**
 * Menus del Sprint 2: insumos, categorias, caducidad y busqueda.
 * Igual que MenuPrincipal, aqui no hay reglas de negocio: todo se le pide a los servicios.
 */
public class MenuInsumos {

    private final AuthService auth;
    private final InsumoService insumoService;
    private final CategoriaService categoriaService;

    public MenuInsumos(AuthService auth) {
        this.auth = auth;
        this.insumoService = new InsumoService(auth);
        this.categoriaService = new CategoriaService(auth);
    }

    // ================== MENU DE INSUMOS ==================

    public void mostrar() {
        int opcion;
        do {
            Usuario u = auth.getUsuarioActual();
            boolean esAdmin = u.puedeModificarInsumos();   // polimorfismo

            Consola.titulo("MODULO DE INSUMOS");
            System.out.println("1. Ver todos los insumos");
            System.out.println("2. Buscar insumo");
            System.out.println("3. Ver alertas de caducidad");
            if (esAdmin) {
                System.out.println("4. Registrar insumo");
                System.out.println("5. Modificar insumo");
                System.out.println("6. Eliminar insumo");
                System.out.println("7. Gestionar categorias");
                System.out.println("8. Gestionar fechas de caducidad");
            }
            System.out.println("0. Volver");
            opcion = Consola.leerEntero("Seleccione una opcion: ");

            if (opcion >= 4 && !esAdmin) {
                System.out.println("Opcion no valida.");
                continue;
            }
            try {
                switch (opcion) {
                    case 1: verTodos(); break;
                    case 2: menuBusqueda(); break;
                    case 3: mostrarAlertas(true); Consola.pausa(); break;
                    case 4: registrar(); break;
                    case 5: modificar(); break;
                    case 6: eliminar(); break;
                    case 7: menuCategorias(); break;
                    case 8: menuCaducidad(); break;
                    case 0: break;
                    default: System.out.println("Opcion no valida.");
                }
            } catch (ValidacionException e) {
                System.out.println("  ERROR: " + e.getMessage());
                Consola.pausa();
            }
        } while (opcion != 0);
    }

    private void verTodos() {
        Consola.titulo("INVENTARIO DE INSUMOS");
        imprimirTabla(insumoService.listar());
        Consola.pausa();
    }

    // ================== GES-4: REGISTRAR / MODIFICAR / ELIMINAR ==================

    private void registrar() throws ValidacionException {
        Consola.titulo("REGISTRAR INSUMO");
        if (categoriaService.listar().isEmpty()) {
            throw new ValidacionException("Primero debe crear al menos una categoria (opcion 7).");
        }
        String nombre = Consola.leerTexto("Nombre: ");
        String descripcion = Consola.leerTexto("Descripcion (opcional): ");
        mostrarCategoriasCortas();
        String categoria = Consola.leerTexto("ID de la categoria: ");
        String unidad = Consola.leerTexto("Unidad de medida (" + UnidadMedida.opciones() + "): ");
        String cantidad = Consola.leerTexto("Cantidad: ");
        String fecha = Consola.leerTexto("Fecha de caducidad (dd/mm/aaaa): ");

        Insumo i = insumoService.registrar(nombre, descripcion, categoria, cantidad, unidad, fecha);
        System.out.println("Insumo '" + i.getNombre() + "' registrado. " + i.textoDias() + ".");
        avisarSiCorresponde(i);
        Consola.pausa();
    }

    private void modificar() throws ValidacionException {
        Consola.titulo("MODIFICAR INSUMO");
        imprimirTabla(insumoService.listar());
        int id = Consola.leerEntero("\nID del insumo a modificar: ");
        Insumo actual = insumoService.buscarPorId(id);

        System.out.println("(Deje en blanco para conservar el valor actual)");
        String nombre = pedir("Nombre", actual.getNombre());
        String descripcion = pedir("Descripcion", actual.getDescripcion());
        mostrarCategoriasCortas();
        String categoria = pedir("ID de la categoria", String.valueOf(actual.getCategoria().getId()));
        String unidad = pedir("Unidad (" + UnidadMedida.opciones() + ")", actual.getUnidad().getSimbolo());
        String cantidad = pedir("Cantidad", actual.getCantidadFormateada());
        String fecha = pedir("Fecha de caducidad (dd/mm/aaaa)", actual.getFechaFormateada());

        Insumo i = insumoService.modificar(id, nombre, descripcion, categoria, cantidad, unidad, fecha);
        System.out.println("Insumo actualizado correctamente.");
        avisarSiCorresponde(i);
        Consola.pausa();
    }

    private void eliminar() throws ValidacionException {
        Consola.titulo("ELIMINAR INSUMO");
        imprimirTabla(insumoService.listar());
        int id = Consola.leerEntero("\nID del insumo a eliminar: ");
        Insumo i = insumoService.buscarPorId(id);
        if (Consola.confirmar("Seguro que desea eliminar '" + i.getNombre() + "'?")) {
            insumoService.eliminar(id);
            System.out.println("Insumo eliminado.");
        } else {
            System.out.println("Operacion cancelada.");
        }
        Consola.pausa();
    }

    // ================== GES-3: BUSQUEDA ==================

    private void menuBusqueda() throws ValidacionException {
        Consola.titulo("BUSCAR INSUMO");
        System.out.println("1. Por nombre");
        System.out.println("2. Por categoria");
        System.out.println("3. Por estado (vigente / por caducar / caducado)");
        System.out.println("0. Volver");
        int op = Consola.leerEntero("Seleccione una opcion: ");

        List<Insumo> resultado;
        switch (op) {
            case 1:
                resultado = insumoService.buscarPorNombre(Consola.leerTexto("Escriba el nombre o parte de el: "));
                break;
            case 2:
                mostrarCategoriasCortas();
                resultado = insumoService.buscarPorCategoria(Consola.leerEntero("ID de la categoria: "));
                break;
            case 3:
                System.out.println("1. Vigente  2. Por caducar  3. Caducado");
                int e = Consola.leerEntero("Estado: ");
                if (e < 1 || e > 3) throw new ValidacionException("Estado no valido.");
                resultado = insumoService.buscarPorEstado(EstadoInsumo.values()[e - 1]);
                break;
            case 0:
                return;
            default:
                System.out.println("Opcion no valida.");
                return;
        }
        System.out.println("\nResultados encontrados: " + resultado.size());
        imprimirTabla(resultado);
        Consola.pausa();
    }

    // ================== GES-24: CATEGORIAS ==================

    private void menuCategorias() {
        int op;
        do {
            Consola.titulo("GESTION DE CATEGORIAS");
            System.out.println("1. Listar categorias");
            System.out.println("2. Crear categoria");
            System.out.println("3. Modificar categoria");
            System.out.println("4. Eliminar categoria");
            System.out.println("0. Volver");
            op = Consola.leerEntero("Seleccione una opcion: ");
            try {
                switch (op) {
                    case 1:
                        listarCategorias();
                        Consola.pausa();
                        break;
                    case 2: {
                        String nombre = Consola.leerTexto("Nombre: ");
                        String desc = Consola.leerTexto("Descripcion (opcional): ");
                        categoriaService.crear(nombre, desc);
                        System.out.println("Categoria creada.");
                        Consola.pausa();
                        break;
                    }
                    case 3: {
                        listarCategorias();
                        int id = Consola.leerEntero("ID de la categoria: ");
                        Categoria c = categoriaService.buscarPorId(id);
                        String nombre = pedir("Nombre", c.getNombre());
                        String desc = pedir("Descripcion", c.getDescripcion());
                        categoriaService.modificar(id, nombre, desc);
                        System.out.println("Categoria actualizada.");
                        Consola.pausa();
                        break;
                    }
                    case 4: {
                        listarCategorias();
                        int id = Consola.leerEntero("ID de la categoria: ");
                        Categoria c = categoriaService.buscarPorId(id);
                        if (Consola.confirmar("Eliminar la categoria '" + c.getNombre() + "'?")) {
                            categoriaService.eliminar(id);
                            System.out.println("Categoria eliminada.");
                        }
                        Consola.pausa();
                        break;
                    }
                    case 0: break;
                    default: System.out.println("Opcion no valida.");
                }
            } catch (ValidacionException e) {
                System.out.println("  ERROR: " + e.getMessage());
                Consola.pausa();
            }
        } while (op != 0);
    }

    private void listarCategorias() {
        List<Categoria> lista = categoriaService.listar();
        System.out.println(String.format("%-4s %-20s %-10s %s", "ID", "NOMBRE", "INSUMOS", "DESCRIPCION"));
        System.out.println("------------------------------------------------------------");
        for (Categoria c : lista) {
            System.out.println(String.format("%-4d %-20s %-10d %s", c.getId(), c.getNombre(),
                    categoriaService.contarInsumos(c.getId()),
                    c.getDescripcion() == null ? "" : c.getDescripcion()));
        }
        if (lista.isEmpty()) System.out.println("(No hay categorias registradas)");
    }

    private void mostrarCategoriasCortas() {
        StringBuilder sb = new StringBuilder("Categorias: ");
        for (Categoria c : categoriaService.listar()) {
            sb.append(c.getId()).append("=").append(c.getNombre()).append("  ");
        }
        System.out.println(sb);
    }

    // ================== GES-2: FECHAS DE CADUCIDAD ==================

    private void menuCaducidad() {
        int op;
        do {
            Consola.titulo("GESTION DE FECHAS DE CADUCIDAD");
            System.out.println("Dias de aviso actuales: " + insumoService.getDiasAviso());
            System.out.println("1. Ver reporte de caducidad");
            System.out.println("2. Actualizar fecha de caducidad de un insumo");
            System.out.println("3. Cambiar dias de aviso");
            System.out.println("4. Eliminar todos los insumos caducados");
            System.out.println("0. Volver");
            op = Consola.leerEntero("Seleccione una opcion: ");
            try {
                switch (op) {
                    case 1:
                        mostrarAlertas(true);
                        Consola.pausa();
                        break;
                    case 2: {
                        imprimirTabla(insumoService.listar());
                        int id = Consola.leerEntero("\nID del insumo: ");
                        Insumo i = insumoService.buscarPorId(id);
                        System.out.println("Fecha actual: " + i.getFechaFormateada() + " (" + i.textoDias() + ")");
                        insumoService.actualizarFechaCaducidad(id, Consola.leerTexto("Nueva fecha (dd/mm/aaaa): "));
                        System.out.println("Fecha actualizada.");
                        Consola.pausa();
                        break;
                    }
                    case 3:
                        insumoService.setDiasAviso(Consola.leerEntero("Avisar con cuantos dias de anticipacion: "));
                        System.out.println("Configuracion guardada.");
                        Consola.pausa();
                        break;
                    case 4: {
                        List<Insumo> caducados = insumoService.listarCaducados();
                        if (caducados.isEmpty()) {
                            System.out.println("No hay insumos caducados.");
                        } else {
                            imprimirTabla(caducados);
                            if (Consola.confirmar("Eliminar estos " + caducados.size() + " insumos?")) {
                                System.out.println("Se eliminaron " + insumoService.eliminarCaducados() + " insumos.");
                            }
                        }
                        Consola.pausa();
                        break;
                    }
                    case 0: break;
                    default: System.out.println("Opcion no valida.");
                }
            } catch (ValidacionException e) {
                System.out.println("  ERROR: " + e.getMessage());
                Consola.pausa();
            }
        } while (op != 0);
    }

    /**
     * Muestra los insumos caducados y por caducar.
     * Se llama al iniciar sesion (resumen) y desde el menu (detalle).
     */
    public void mostrarAlertas(boolean siempreMostrar) {
        List<Insumo> caducados = insumoService.listarCaducados();
        List<Insumo> porCaducar = insumoService.listarPorCaducar();
        int dias = insumoService.getDiasAviso();

        if (caducados.isEmpty() && porCaducar.isEmpty()) {
            if (siempreMostrar) {
                System.out.println("\nTodo en orden: ningun insumo vence en los proximos " + dias + " dias.");
            }
            return;
        }

        System.out.println();
        System.out.println("**************************************************");
        System.out.println("  !! ALERTA DE CADUCIDAD !!");
        System.out.println("**************************************************");
        if (!caducados.isEmpty()) {
            System.out.println("\nCADUCADOS (" + caducados.size() + "):");
            for (Insumo i : caducados) {
                System.out.println("  - " + i.getNombre() + " | " + i.getFechaFormateada() + " | " + i.textoDias());
            }
        }
        if (!porCaducar.isEmpty()) {
            System.out.println("\nPOR CADUCAR en los proximos " + dias + " dias (" + porCaducar.size() + "):");
            for (Insumo i : porCaducar) {
                System.out.println("  - " + i.getNombre() + " | " + i.getFechaFormateada() + " | " + i.textoDias());
            }
        }
        System.out.println("**************************************************");
    }

    // ================== METODOS DE APOYO ==================

    private void imprimirTabla(List<Insumo> lista) {
        if (lista.isEmpty()) {
            System.out.println("(No hay insumos para mostrar)");
            return;
        }
        int dias = insumoService.getDiasAviso();
        String formato = "%-4s %-22s %-14s %-14s %-11s %-18s %s%n";
        System.out.printf(formato, "ID", "NOMBRE", "CATEGORIA", "CANTIDAD", "CADUCA", "TIEMPO", "ESTADO");
        System.out.println("---------------------------------------------------------------------------------------------------");
        for (Insumo i : lista) {
            System.out.printf(formato,
                    i.getId(),
                    recortar(i.getNombre(), 22),
                    recortar(i.getCategoria().getNombre(), 14),
                    i.getCantidadFormateada() + " " + i.getUnidad().getSimbolo(),
                    i.getFechaFormateada(),
                    i.textoDias(),
                    i.getEstado(dias).getDescripcion());
        }
    }

    private void avisarSiCorresponde(Insumo i) {
        EstadoInsumo estado = i.getEstado(insumoService.getDiasAviso());
        if (estado == EstadoInsumo.CADUCADO) {
            System.out.println("  !! Atencion: este insumo ya esta CADUCADO.");
        } else if (estado == EstadoInsumo.POR_CADUCAR) {
            System.out.println("  !! Atencion: este insumo esta proximo a caducar.");
        }
    }

    private String pedir(String campo, String actual) {
        String entrada = Consola.leerTexto(campo + " [" + (actual == null ? "" : actual) + "]: ");
        return entrada.isEmpty() ? actual : entrada;
    }

    private String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
