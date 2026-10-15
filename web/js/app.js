/* =============================================================
   Lógica de la página. Habla con el servidor Java en /api/...
   (todas las reglas y validaciones viven en el servidor).
   ============================================================= */
'use strict';

const estado = {
  usuario: null,      // usuario con sesión iniciada
  categorias: [],
  unidades: [],
  insumos: [],
};

// ---------------------------------------------------------------
//  Utilidades
// ---------------------------------------------------------------

/** Llama a la API. Si se pasan datos, hace POST como formulario. */
async function api(ruta, datos) {
  const opciones = { credentials: 'same-origin' };
  if (datos !== undefined) {
    opciones.method = 'POST';
    opciones.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    opciones.body = new URLSearchParams(datos);
  }
  const resp = await fetch('/api' + ruta, opciones);
  const json = await resp.json().catch(() => ({ error: 'Respuesta inválida del servidor.' }));
  if (resp.status === 401 && ruta !== '/login') {
    mostrarPantalla('vista-login');
    throw new Error('Su sesión terminó. Inicie sesión de nuevo.');
  }
  if (!resp.ok) throw new Error(json.error || 'Error ' + resp.status);
  return json;
}

/** Escapa texto para insertarlo en HTML sin riesgo (evita XSS). */
function esc(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

function avisar(mensaje, esError = false) {
  const div = document.createElement('div');
  div.className = 'aviso' + (esError ? ' aviso-error' : '');
  div.textContent = mensaje;
  document.getElementById('avisos').appendChild(div);
  setTimeout(() => div.remove(), esError ? 6000 : 3500);
}

function datosDe(form) {
  return Object.fromEntries(new FormData(form));
}

/** Conecta un formulario: al enviarlo ejecuta la función y muestra el mensaje o el error. */
function alEnviar(id, funcion) {
  const form = document.getElementById(id);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const boton = form.querySelector('[type=submit]');
    if (boton) boton.disabled = true;
    try {
      const r = await funcion(datosDe(form), form);
      if (r && r.mensaje) avisar(r.mensaje);
    } catch (err) {
      avisar(err.message, true);
    } finally {
      if (boton) boton.disabled = false;
    }
  });
}

function esAdmin() {
  return estado.usuario && estado.usuario.permisos.modificarInsumos;
}

function cantidadTexto(i) {
  return `${i.cantidad} ${i.unidadSimbolo}`;
}

function etiquetaEstado(i) {
  return `<span class="estado estado-${esc(i.estado)}">${esc(i.estadoTexto)}</span>`;
}

// ---------------------------------------------------------------
//  Navegación entre pantallas y secciones
// ---------------------------------------------------------------

function mostrarPantalla(id) {
  for (const p of ['vista-login', 'vista-recuperar', 'vista-app']) {
    document.getElementById(p).classList.toggle('oculto', p !== id);
  }
}

function mostrarSeccion(nombre) {
  document.querySelectorAll('.seccion').forEach(s => s.classList.add('oculto'));
  document.getElementById('seccion-' + nombre).classList.remove('oculto');
  document.querySelectorAll('#menu button').forEach(b =>
    b.classList.toggle('activo', b.dataset.seccion === nombre));
  const cargar = {
    inicio: cargarInicio, insumos: cargarInsumos, categorias: cargarCategorias,
    caducidad: cargarCaducidad, usuarios: cargarUsuarios, perfil: cargarPerfil,
  }[nombre];
  cargar().catch(err => avisar(err.message, true));
}

document.querySelectorAll('#menu button').forEach(b =>
  b.addEventListener('click', () => mostrarSeccion(b.dataset.seccion)));

document.querySelectorAll('[data-ir]').forEach(b =>
  b.addEventListener('click', () => {
    mostrarPantalla(b.dataset.ir);
    document.getElementById('form-recuperar-1').classList.remove('oculto');
    document.getElementById('form-recuperar-2').classList.add('oculto');
  }));

/** Prepara la aplicación para el usuario que inició sesión (permisos por rol). */
async function entrar(usuario) {
  estado.usuario = usuario;
  document.getElementById('nombre-usuario').textContent = usuario.nombre;
  document.getElementById('rol-usuario').textContent = usuario.rolDescripcion;
  // GES-22: las opciones de administrador solo se muestran si el rol lo permite
  document.querySelectorAll('.solo-admin').forEach(el => el.classList.toggle('oculto', !esAdmin()));
  document.querySelector('#menu [data-seccion=usuarios]')
    .classList.toggle('oculto', !usuario.permisos.gestionarUsuarios);

  [estado.categorias, estado.unidades] = await Promise.all([api('/categorias'), api('/unidades')]);
  llenarSelects();
  mostrarPantalla('vista-app');
  mostrarSeccion('inicio');
}

// ---------------------------------------------------------------
//  GES-21 Iniciar sesión · GES-7 Cerrar sesión · GES-23 Recuperar
// ---------------------------------------------------------------

alEnviar('form-login', async (datos, form) => {
  const r = await api('/login', datos);
  form.reset();
  await entrar(r.usuario);
  return r;
});

document.getElementById('boton-salir').addEventListener('click', async () => {
  if (!confirm('¿Seguro que desea cerrar sesión?')) return;
  await api('/logout', {}).catch(() => {});
  estado.usuario = null;
  mostrarPantalla('vista-login');
  avisar('Sesión cerrada.');
});

let usuarioRecuperar = '';
alEnviar('form-recuperar-1', async (datos) => {
  const r = await api('/recuperar/pregunta', datos);
  usuarioRecuperar = datos.username;
  document.getElementById('texto-pregunta').textContent = r.pregunta;
  document.getElementById('form-recuperar-1').classList.add('oculto');
  document.getElementById('form-recuperar-2').classList.remove('oculto');
});

alEnviar('form-recuperar-2', async (datos, form) => {
  const r = await api('/recuperar', { ...datos, username: usuarioRecuperar });
  form.reset();
  mostrarPantalla('vista-login');
  return r;
});

// ---------------------------------------------------------------
//  Inicio: resumen y alertas (GES-2)
// ---------------------------------------------------------------

async function cargarInicio() {
  const [res, alertas] = await Promise.all([api('/resumen'), api('/alertas')]);
  document.getElementById('r-total').textContent = res.total;
  document.getElementById('r-vigentes').textContent = res.vigentes;
  document.getElementById('r-por-caducar').textContent = res.porCaducar;
  document.getElementById('r-caducados').textContent = res.caducados;
  document.getElementById('r-dias').textContent = res.diasAviso;

  const lista = items => items.map(i =>
    `<li><strong>${esc(i.nombre)}</strong> — ${esc(cantidadTexto(i))} · ${esc(i.fechaTexto)} · ${esc(i.tiempo)}</li>`).join('');

  let html = '';
  if (alertas.caducados.length) {
    html += `<div class="alerta alerta-CADUCADO"><h3>Caducados (${alertas.caducados.length})</h3>
             <ul>${lista(alertas.caducados)}</ul></div>`;
  }
  if (alertas.porCaducar.length) {
    html += `<div class="alerta alerta-POR_CADUCAR"><h3>Por caducar en los próximos ${alertas.diasAviso} días (${alertas.porCaducar.length})</h3>
             <ul>${lista(alertas.porCaducar)}</ul></div>`;
  }
  if (!html) {
    html = `<div class="alerta alerta-ok">Todo en orden: ningún insumo vence en los próximos ${alertas.diasAviso} días.</div>`;
  }
  document.getElementById('alertas').innerHTML = html;
}

// ---------------------------------------------------------------
//  Insumos: listado, búsqueda (GES-3) y CRUD (GES-4, GES-25)
// ---------------------------------------------------------------

function llenarSelects() {
  const opcionesCat = estado.categorias.map(c => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('');
  document.querySelectorAll('.select-categorias').forEach(sel => {
    const primera = sel.closest('#form-busqueda') ? '<option value="">Todas las categorías</option>' : '';
    const valor = sel.value;
    sel.innerHTML = primera + opcionesCat;
    sel.value = valor;
  });
  document.getElementById('select-unidades').innerHTML =
    estado.unidades.map(u => `<option value="${u.codigo}">${esc(u.simbolo)}</option>`).join('');
}

async function cargarInsumos() {
  const filtros = new URLSearchParams(datosDe(document.getElementById('form-busqueda')));
  estado.insumos = await api('/insumos?' + filtros);
  const tbody = document.getElementById('tabla-insumos');
  const hayFiltro = [...filtros.values()].some(v => v);
  document.getElementById('conteo-insumos').textContent =
    `${estado.insumos.length} insumo(s)${hayFiltro ? ' encontrados' : ''}`;

  if (!estado.insumos.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="vacio">${hayFiltro ? 'No se encontraron insumos con esos filtros.' : 'No hay insumos registrados.'}</td></tr>`;
    return;
  }
  tbody.innerHTML = estado.insumos.map(i => `
    <tr>
      <td><strong>${esc(i.nombre)}</strong>${i.descripcion ? `<span class="descripcion-insumo">${esc(i.descripcion)}</span>` : ''}</td>
      <td>${esc(i.categoria)}</td>
      <td class="num">${esc(cantidadTexto(i))}</td>
      <td>${esc(i.fechaTexto)}</td>
      <td>${esc(i.tiempo)}</td>
      <td>${etiquetaEstado(i)}</td>
      <td class="acciones ${esAdmin() ? '' : 'oculto'}">
        <button class="boton boton-secundario boton-pequeno" data-editar="${i.id}">Editar</button>
        <button class="boton boton-peligro boton-pequeno" data-eliminar="${i.id}">Eliminar</button>
      </td>
    </tr>`).join('');
}

document.getElementById('form-busqueda').addEventListener('submit', (e) => {
  e.preventDefault();
  cargarInsumos().catch(err => avisar(err.message, true));
});
document.getElementById('form-busqueda').addEventListener('reset', () =>
  setTimeout(() => cargarInsumos().catch(err => avisar(err.message, true))));

document.getElementById('tabla-insumos').addEventListener('click', async (e) => {
  const idEditar = e.target.dataset.editar;
  const idEliminar = e.target.dataset.eliminar;
  if (idEditar) abrirDialogoInsumo(estado.insumos.find(i => i.id == idEditar));
  if (idEliminar) {
    const ins = estado.insumos.find(i => i.id == idEliminar);
    if (!confirm(`¿Eliminar "${ins.nombre}"? Esta acción no se puede deshacer.`)) return;
    try {
      avisar((await api('/insumos/eliminar', { id: idEliminar })).mensaje);
      cargarInsumos();
    } catch (err) { avisar(err.message, true); }
  }
});

const dialogo = document.getElementById('dialogo-insumo');
const formInsumo = document.getElementById('form-insumo');

function abrirDialogoInsumo(insumo) {
  if (!estado.categorias.length) {
    avisar('Primero cree al menos una categoría.', true);
    return;
  }
  formInsumo.reset();
  document.getElementById('error-insumo').textContent = '';
  document.getElementById('titulo-dialogo-insumo').textContent = insumo ? 'Modificar insumo' : 'Nuevo insumo';
  formInsumo.id.value = insumo ? insumo.id : '';
  if (insumo) {
    formInsumo.nombre.value = insumo.nombre;
    formInsumo.descripcion.value = insumo.descripcion;
    formInsumo.categoria.value = insumo.categoriaId;
    formInsumo.fecha.value = insumo.fecha;
    formInsumo.cantidad.value = insumo.cantidad;
    formInsumo.unidad.value = insumo.unidad;
  }
  actualizarPasoCantidad();
  dialogo.showModal();
}

/** Si la unidad no permite decimales (unidad, caja, paquete), el campo solo acepta enteros. */
function actualizarPasoCantidad() {
  const u = estado.unidades.find(x => x.codigo === formInsumo.unidad.value);
  formInsumo.cantidad.step = u && !u.decimales ? '1' : 'any';
}
formInsumo.unidad.addEventListener('change', actualizarPasoCantidad);

document.getElementById('boton-nuevo-insumo').addEventListener('click', () => abrirDialogoInsumo(null));
document.getElementById('boton-cancelar-insumo').addEventListener('click', () => dialogo.close());

formInsumo.addEventListener('submit', async (e) => {
  e.preventDefault();
  const datos = datosDe(formInsumo);
  const ruta = datos.id ? '/insumos/modificar' : '/insumos';
  try {
    const r = await api(ruta, datos);
    dialogo.close();
    avisar(r.mensaje);
    if (r.insumo.estado !== 'VIGENTE') avisar(`Atención: "${r.insumo.nombre}" está ${r.insumo.estadoTexto.toLowerCase()}.`, true);
    cargarInsumos();
  } catch (err) {
    // GES-25: el mensaje de validación del servidor se muestra dentro del formulario
    document.getElementById('error-insumo').textContent = err.message;
  }
});

// ---------------------------------------------------------------
//  Categorías (GES-24)
// ---------------------------------------------------------------

async function cargarCategorias() {
  estado.categorias = await api('/categorias');
  llenarSelects();
  const tbody = document.getElementById('tabla-categorias');
  if (!estado.categorias.length) {
    tbody.innerHTML = '<tr><td colspan="4" class="vacio">No hay categorías.</td></tr>';
    return;
  }
  tbody.innerHTML = estado.categorias.map(c => `
    <tr>
      <td><strong>${esc(c.nombre)}</strong></td>
      <td>${esc(c.descripcion)}</td>
      <td class="num">${c.insumos}</td>
      <td class="acciones">
        <button class="boton boton-secundario boton-pequeno" data-editar="${c.id}">Editar</button>
        <button class="boton boton-peligro boton-pequeno" data-eliminar="${c.id}">Eliminar</button>
      </td>
    </tr>`).join('');
}

function limpiarFormCategoria() {
  const f = document.getElementById('form-categoria');
  f.reset();
  f.id.value = '';
  document.getElementById('boton-guardar-categoria').textContent = 'Crear categoría';
  document.getElementById('boton-cancelar-categoria').classList.add('oculto');
}

alEnviar('form-categoria', async (datos) => {
  const r = await api(datos.id ? '/categorias/modificar' : '/categorias', datos);
  limpiarFormCategoria();
  await cargarCategorias();
  return r;
});
document.getElementById('boton-cancelar-categoria').addEventListener('click', limpiarFormCategoria);

document.getElementById('tabla-categorias').addEventListener('click', async (e) => {
  const c = estado.categorias.find(x => x.id == (e.target.dataset.editar || e.target.dataset.eliminar));
  if (!c) return;
  if (e.target.dataset.editar) {
    const f = document.getElementById('form-categoria');
    f.id.value = c.id;
    f.nombre.value = c.nombre;
    f.descripcion.value = c.descripcion;
    document.getElementById('boton-guardar-categoria').textContent = 'Guardar cambios';
    document.getElementById('boton-cancelar-categoria').classList.remove('oculto');
    f.nombre.focus();
  } else {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return;
    try {
      avisar((await api('/categorias/eliminar', { id: c.id })).mensaje);
      cargarCategorias();
    } catch (err) { avisar(err.message, true); }
  }
});

// ---------------------------------------------------------------
//  Caducidad (GES-2)
// ---------------------------------------------------------------

async function cargarCaducidad() {
  const [res, insumos, alertas] = await Promise.all([api('/resumen'), api('/insumos'), api('/alertas')]);
  document.querySelector('#form-dias [name=dias]').value = res.diasAviso;
  document.getElementById('select-insumo-fecha').innerHTML = insumos.map(i =>
    `<option value="${i.id}">${esc(i.nombre)} — ${esc(i.fechaTexto)} (${esc(i.estadoTexto)})</option>`).join('');

  const cont = document.getElementById('lista-caducados');
  document.getElementById('boton-eliminar-caducados').disabled = !alertas.caducados.length;
  cont.innerHTML = alertas.caducados.length
    ? `<ul>${alertas.caducados.map(i => `<li><strong>${esc(i.nombre)}</strong> — ${esc(i.fechaTexto)} · ${esc(i.tiempo)}</li>`).join('')}</ul>`
    : '<p class="texto-suave">No hay insumos caducados.</p>';
}

alEnviar('form-dias', async (datos) => {
  const r = await api('/caducidad/dias', datos);
  await cargarCaducidad();
  return r;
});

alEnviar('form-fecha', async (datos, form) => {
  const r = await api('/caducidad/fecha', datos);
  form.fecha.value = '';
  await cargarCaducidad();
  return r;
});

document.getElementById('boton-eliminar-caducados').addEventListener('click', async () => {
  if (!confirm('¿Eliminar TODOS los insumos caducados?')) return;
  try {
    avisar((await api('/caducidad/eliminar-caducados', {})).mensaje);
    cargarCaducidad();
  } catch (err) { avisar(err.message, true); }
});

// ---------------------------------------------------------------
//  Usuarios y roles (GES-22)
// ---------------------------------------------------------------

async function cargarUsuarios() {
  const usuarios = await api('/usuarios');
  document.getElementById('tabla-usuarios').innerHTML = usuarios.map(u => {
    const yo = u.id === estado.usuario.id;
    return `
    <tr>
      <td><strong>${esc(u.username)}</strong>${yo ? ' <span class="etiqueta">Tú</span>' : ''}</td>
      <td>${esc(u.nombre)}</td>
      <td>
        <select data-rol="${u.id}" ${yo ? 'disabled' : ''} aria-label="Rol de ${esc(u.username)}">
          <option value="ADMIN" ${u.rol === 'ADMIN' ? 'selected' : ''}>Administrador</option>
          <option value="EMPLEADO" ${u.rol === 'EMPLEADO' ? 'selected' : ''}>Empleado</option>
        </select>
      </td>
      <td><span class="estado ${u.activo ? 'estado-VIGENTE' : 'estado-CADUCADO'}">${u.activo ? 'Activo' : 'Inactivo'}</span></td>
      <td class="acciones">
        ${yo ? '' : `<button class="boton boton-secundario boton-pequeno" data-estado="${u.id}" data-activo="${!u.activo}">
          ${u.activo ? 'Desactivar' : 'Activar'}</button>`}
      </td>
    </tr>`;
  }).join('');
}

alEnviar('form-usuario', async (datos, form) => {
  const r = await api('/usuarios', datos);
  form.reset();
  await cargarUsuarios();
  return r;
});

document.getElementById('tabla-usuarios').addEventListener('change', async (e) => {
  if (!e.target.dataset.rol) return;
  try {
    avisar((await api('/usuarios/rol', { id: e.target.dataset.rol, rol: e.target.value })).mensaje);
  } catch (err) { avisar(err.message, true); }
  cargarUsuarios();
});

document.getElementById('tabla-usuarios').addEventListener('click', async (e) => {
  const id = e.target.dataset.estado;
  if (!id) return;
  try {
    avisar((await api('/usuarios/estado', { id, activo: e.target.dataset.activo })).mensaje);
    cargarUsuarios();
  } catch (err) { avisar(err.message, true); }
});

// ---------------------------------------------------------------
//  Mi perfil (GES-14)
// ---------------------------------------------------------------

async function cargarPerfil() {
  const u = await api('/perfil');
  const f = document.getElementById('form-perfil');
  f.username.value = u.username;
  f.nombre.value = u.nombre;
  f.email.value = u.email;
  f.telefono.value = u.telefono;
  document.querySelector('#form-pregunta [name=pregunta]').value = u.pregunta;
}

alEnviar('form-perfil', async (datos) => {
  const r = await api('/perfil', datos);
  const u = await api('/perfil');
  estado.usuario.nombre = u.nombre;
  document.getElementById('nombre-usuario').textContent = u.nombre;
  return r;
});

alEnviar('form-password', async (datos, form) => {
  const r = await api('/perfil/password', datos);
  form.reset();
  return r;
});

alEnviar('form-pregunta', async (datos, form) => {
  const r = await api('/perfil/pregunta', datos);
  form.respuesta.value = '';
  return r;
});

// ---------------------------------------------------------------
//  Arranque: si ya hay sesión abierta, entrar directo
// ---------------------------------------------------------------

(async function iniciar() {
  try {
    const s = await api('/sesion');
    if (s.autenticado) await entrar(s.usuario);
    else mostrarPantalla('vista-login');
  } catch {
    mostrarPantalla('vista-login');
  }
})();
