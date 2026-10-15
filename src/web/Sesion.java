package web;

import servicio.AuthService;
import servicio.CategoriaService;
import servicio.InsumoService;
import servicio.UsuarioService;

/**
 * Sesion de un usuario en el navegador.
 * En consola habia UN solo AuthService; en la web cada persona conectada tiene el suyo,
 * asi se reutilizan exactamente los mismos servicios del Sprint 1 y 2.
 */
public class Sesion {

    private final String token;
    private final AuthService auth = new AuthService();
    private final UsuarioService usuarios = new UsuarioService(auth);
    private final InsumoService insumos = new InsumoService(auth);
    private final CategoriaService categorias = new CategoriaService(auth);
    private long ultimoAcceso = System.currentTimeMillis();

    public Sesion(String token) {
        this.token = token;
    }

    public void tocar() {
        ultimoAcceso = System.currentTimeMillis();
    }

    public boolean expiro(long minutos) {
        return System.currentTimeMillis() - ultimoAcceso > minutos * 60_000;
    }

    public String getToken() { return token; }
    public AuthService getAuth() { return auth; }
    public UsuarioService getUsuarios() { return usuarios; }
    public InsumoService getInsumos() { return insumos; }
    public CategoriaService getCategorias() { return categorias; }
}
