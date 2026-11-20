'use strict';

/* ---------- Utilidades ---------- */
const $ = (s, el = document) => el.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(n || 0);
const fmtDate = f => f ? new Date(f).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' }) : '—';
const STOCK_MIN = 10; // mismo umbral que ReporteService (stock < 10)
const view = $('#view');

function localISO(d = new Date()) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

function toast(msg, type = 'ok') {
  const t = document.createElement('div');
  t.className = 'toast ' + (type === 'error' ? 'error' : '');
  t.textContent = msg;
  $('#toasts').appendChild(t);
  setTimeout(() => t.remove(), type === 'error' ? 6000 : 3000);
}

/* ---------- API ---------- */
async function api(path, { method = 'GET', body } = {}) {
  let res;
  try {
    res = await fetch('/api' + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor. Revisa que la aplicación esté corriendo en el puerto 9090.');
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* sin cuerpo JSON */ }
  if (!res.ok) {
    let msg = data && (data.message || data.detail);
    if (!msg || msg === 'No message available') {
      msg = res.status === 409 ? 'Ya existe un registro con esos datos.'
        : res.status === 500 ? 'El servidor no pudo completar la operación. Si intentas eliminar, el registro puede estar en uso por otros.'
        : 'Error ' + res.status;
    }
    throw new Error(msg);
  }
  return data;
}

/* ---------- Diálogo de formulario / confirmación ---------- */
function openForm({ title, fields = [], values = {}, message = '', submitLabel = 'Guardar', danger = false }) {
  return new Promise(resolve => {
    const dlg = $('#dlg');
    let done = false;
    const finish = v => { if (!done) { done = true; resolve(v); } };
    const fieldHTML = f => {
      const v = values[f.name] ?? '';
      const id = 'f_' + f.name;
      if (f.type === 'checkbox') {
        return `<div class="field inline"><input type="checkbox" id="${id}" name="${f.name}" ${values[f.name] ? 'checked' : ''}><label for="${id}">${esc(f.label)}</label></div>`;
      }
      let control;
      if (f.type === 'select') {
        control = `<select id="${id}" name="${f.name}" ${f.required ? 'required' : ''}>${f.options.map(o =>
          `<option value="${esc(o.value)}" ${String(o.value) === String(v) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`;
      } else {
        control = `<input id="${id}" name="${f.name}" type="${f.type || 'text'}" value="${esc(v)}" ${f.required ? 'required' : ''}
          ${f.min != null ? `min="${f.min}"` : ''} ${f.step ? `step="${f.step}"` : ''} ${f.maxlength ? `maxlength="${f.maxlength}"` : ''} autocomplete="off">`;
      }
      return `<div class="field"><label for="${id}">${esc(f.label)}</label>${control}</div>`;
    };
    dlg.innerHTML = `<form class="dlg-form">
      <h2>${esc(title)}</h2>
      ${message ? `<p>${esc(message)}</p>` : ''}
      ${fields.map(fieldHTML).join('')}
      <div class="dlg-actions">
        <button type="button" class="btn" data-cancel>Cancelar</button>
        <button type="submit" class="btn ${danger ? 'danger' : 'primary'}">${esc(submitLabel)}</button>
      </div></form>`;
    const form = $('form', dlg);
    $('[data-cancel]', dlg).onclick = () => dlg.close();
    form.onsubmit = e => {
      e.preventDefault();
      if (!form.reportValidity()) return;
      const out = {};
      fields.forEach(f => {
        const el = form.elements[f.name];
        out[f.name] = f.type === 'checkbox' ? el.checked : el.value.trim();
      });
      finish(out);
      dlg.close();
    };
    dlg.onclose = () => finish(null);
    dlg.showModal();
    const first = form.querySelector('input,select');
    if (first) first.focus();
  });
}

/* ---------- Helpers de interfaz ---------- */
const head = (title, sub = '', actions = '') =>
  `<header class="page-head"><div><h1>${esc(title)}</h1>${sub ? `<p>${esc(sub)}</p>` : ''}</div><div class="actions">${actions}</div></header>`;
const emptyRow = (cols, title, text) =>
  `<tr><td colspan="${cols}"><div class="empty"><strong>${esc(title)}</strong>${esc(text)}</div></td></tr>`;

function stockPill(stock) {
  if (stock <= 0) return '<span class="pill out">Agotado</span>';
  if (stock < STOCK_MIN) return '<span class="pill low">Stock bajo</span>';
  return '';
}

async function guarded(fn, okMsg) {
  try {
    const r = await fn();
    if (okMsg) toast(okMsg);
    return r === undefined ? true : r;
  } catch (e) {
    toast(e.message, 'error');
    return false;
  }
}

function downloadCSV(filename, headers, rows) {
  const q = v => `"${String(v ?? '').replace(/"/g, '""')}"`;
  const csv = '\ufeff' + [headers, ...rows].map(r => r.map(q).join(',')).join('\r\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

/* ---------- Resumen ---------- */
async function dashboard() {
  const [productos, bajos, movs] = await Promise.all([
    api('/productos'), api('/reportes/stock-bajo'), api('/movimientos')
  ]);
  const valor = productos.reduce((s, p) => s + (p.precio || 0) * (p.stock || 0), 0);
  const hoy = new Date().toDateString();
  const movHoy = movs.filter(m => m.fecha && new Date(m.fecha).toDateString() === hoy).length;
  const codigoDe = new Map(productos.map(p => [p.id, p.codigo]));
  const ultimos = [...movs].sort((a, b) => b.id - a.id).slice(0, 8);
  const porReponer = [...bajos].sort((a, b) => a.stock - b.stock).slice(0, 8);

  view.innerHTML = `
    ${head('Resumen', 'Estado actual del inventario', '<a class="btn primary" href="#/movimientos">Registrar movimiento</a>')}
    <section class="panel stats">
      <div class="stat"><span>Productos</span><b>${productos.length}</b></div>
      <div class="stat ${bajos.length ? 'alert' : ''}"><span>Con stock bajo</span><b>${bajos.length}</b></div>
      <div class="stat"><span>Valor del inventario</span><b>${money(valor)}</b></div>
      <div class="stat"><span>Movimientos hoy</span><b>${movHoy}</b></div>
    </section>
    <div class="cols">
      <section class="panel">
        <h2>Por reponer</h2>
        ${porReponer.length ? `<ul class="restock">${porReponer.map(p => `
          <li><div><strong>${esc(p.nombre)}</strong><br><small>${esc(p.codigo ?? codigoDe.get(p.id))}</small></div>
          <div class="num"><strong>${p.stock}</strong> <small>unid.</small></div>
          <div class="meter"><i class="${p.stock <= 0 ? 'zero' : ''}" style="width:${Math.max(4, Math.min(100, p.stock / STOCK_MIN * 100))}%"></i></div></li>`).join('')}</ul>`
          : '<div class="empty"><strong>Todo en orden</strong>Ningún producto está por debajo de ' + STOCK_MIN + ' unidades.</div>'}
      </section>
      <section class="panel">
        <h2>Últimos movimientos</h2>
        <div class="table-wrap"><table><tbody>${ultimos.length ? ultimos.map(m => `
          <tr><td>${fmtDate(m.fecha)}</td><td class="wrap">${esc(m.producto?.nombre)}</td>
          <td class="num ${m.tipo === 'ENTRADA' ? 'mv-in' : 'mv-out'}">${m.tipo === 'ENTRADA' ? '+' : '−'}${m.cantidad}</td></tr>`).join('')
          : emptyRow(3, 'Sin movimientos', 'Registra la primera entrada o salida.')}</tbody></table></div>
      </section>
    </div>`;
}

/* ---------- Productos ---------- */
async function productos() {
  let [lista, cats] = await Promise.all([api('/productos'), api('/categorias')]);
  const reload = async () => { lista = await api('/productos'); paint(); };

  view.innerHTML = `
    ${head('Productos', '', '<button class="btn primary" id="nuevo">Nuevo producto</button>')}
    <div class="toolbar">
      <input type="search" id="q" placeholder="Buscar por nombre o código" aria-label="Buscar producto">
      <select id="cat" aria-label="Filtrar por categoría"><option value="">Todas las categorías</option>
        ${cats.map(c => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}</select>
      <label class="check"><input type="checkbox" id="bajo"> Solo stock bajo</label>
    </div>
    <div class="panel table-wrap"><table>
      <thead><tr><th>Código</th><th>Nombre</th><th>Categoría</th><th class="num">Precio</th><th class="num">Stock</th><th></th></tr></thead>
      <tbody id="rows"></tbody></table></div>`;

  function paint() {
    const q = $('#q').value.trim().toLowerCase(), cat = $('#cat').value, bajo = $('#bajo').checked;
    const rows = lista.filter(p =>
      (!q || p.nombre?.toLowerCase().includes(q) || p.codigo?.toLowerCase().includes(q)) &&
      (!cat || String(p.categoria?.id) === cat) &&
      (!bajo || p.stock < STOCK_MIN)
    ).sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''));
    $('#rows').innerHTML = rows.length ? rows.map(p => `
      <tr><td>${esc(p.codigo)}</td><td>${esc(p.nombre)}</td><td>${esc(p.categoria?.nombre) || '—'}</td>
      <td class="num">${money(p.precio)}</td>
      <td class="num">${p.stock} ${stockPill(p.stock)}</td>
      <td class="row-actions"><button class="btn link" data-edit="${p.id}">Editar</button><button class="btn link del" data-del="${p.id}">Eliminar</button></td></tr>`).join('')
      : emptyRow(6, lista.length ? 'Sin resultados' : 'Aún no hay productos', lista.length ? 'Prueba con otro filtro.' : 'Crea el primero con “Nuevo producto”.');
  }

  const fields = () => [
    { name: 'codigo', label: 'Código', required: true, maxlength: 50 },
    { name: 'nombre', label: 'Nombre', required: true },
    { name: 'categoriaId', label: 'Categoría', type: 'select', options: [{ value: '', label: 'Sin categoría' }, ...cats.map(c => ({ value: c.id, label: c.nombre }))] },
    { name: 'precio', label: 'Precio (S/)', type: 'number', min: 0, step: '0.01', required: true },
    { name: 'stock', label: 'Stock inicial', type: 'number', min: 0, step: '1', required: true }
  ];
  const toBody = v => ({
    codigo: v.codigo, nombre: v.nombre, precio: parseFloat(v.precio), stock: parseInt(v.stock, 10),
    categoria: v.categoriaId ? { id: Number(v.categoriaId) } : null
  });

  async function form(p) {
    const f = fields();
    if (p) f[4].label = 'Stock'; // al editar
    const v = await openForm({
      title: p ? 'Editar producto' : 'Nuevo producto', fields: f,
      values: p ? { ...p, categoriaId: p.categoria?.id ?? '' } : {}
    });
    if (!v) return;
    const ok = await guarded(() => p
      ? api('/productos/' + p.id, { method: 'PUT', body: toBody(v) })
      : api('/productos', { method: 'POST', body: toBody(v) }), p ? 'Producto actualizado' : 'Producto creado');
    if (ok) reload();
  }

  ['q', 'cat', 'bajo'].forEach(id => $('#' + id).addEventListener('input', paint));
  $('#nuevo').onclick = () => form();
  $('#rows').onclick = async e => {
    const ed = e.target.closest('[data-edit]'), del = e.target.closest('[data-del]');
    if (ed) form(lista.find(p => p.id == ed.dataset.edit));
    if (del) {
      const p = lista.find(x => x.id == del.dataset.del);
      const v = await openForm({ title: 'Eliminar producto', message: `Se eliminará “${p.nombre}”. Si tiene movimientos registrados, el sistema no permitirá borrarlo.`, submitLabel: 'Eliminar', danger: true });
      if (v && await guarded(() => api('/productos/' + p.id, { method: 'DELETE' }), 'Producto eliminado')) reload();
    }
  };
  paint();
}

/* ---------- Categorías ---------- */
async function categorias() {
  let lista = await api('/categorias');
  view.innerHTML = `
    ${head('Categorías', '', '<button class="btn primary" id="nuevo">Nueva categoría</button>')}
    <div class="panel table-wrap"><table>
      <thead><tr><th>Nombre</th><th>Descripción</th><th></th></tr></thead><tbody id="rows"></tbody></table></div>`;
  const paint = () => {
    $('#rows').innerHTML = lista.length ? lista.map(c => `
      <tr><td>${esc(c.nombre)}</td><td class="wrap">${esc(c.descripcion) || '—'}</td>
      <td class="row-actions"><button class="btn link" data-edit="${c.id}">Editar</button><button class="btn link del" data-del="${c.id}">Eliminar</button></td></tr>`).join('')
      : emptyRow(3, 'Aún no hay categorías', 'Crea una para agrupar tus productos.');
  };
  const reload = async () => { lista = await api('/categorias'); paint(); };
  async function form(c) {
    const v = await openForm({
      title: c ? 'Editar categoría' : 'Nueva categoría', values: c || {},
      fields: [{ name: 'nombre', label: 'Nombre', required: true }, { name: 'descripcion', label: 'Descripción' }]
    });
    if (!v) return;
    if (await guarded(() => api(c ? '/categorias/' + c.id : '/categorias', { method: c ? 'PUT' : 'POST', body: v }), c ? 'Categoría actualizada' : 'Categoría creada')) reload();
  }
  $('#nuevo').onclick = () => form();
  $('#rows').onclick = async e => {
    const ed = e.target.closest('[data-edit]'), del = e.target.closest('[data-del]');
    if (ed) form(lista.find(c => c.id == ed.dataset.edit));
    if (del) {
      const c = lista.find(x => x.id == del.dataset.del);
      const v = await openForm({ title: 'Eliminar categoría', message: `Se eliminará “${c.nombre}”. Si tiene productos asignados, el sistema no permitirá borrarla.`, submitLabel: 'Eliminar', danger: true });
      if (v && await guarded(() => api('/categorias/' + c.id, { method: 'DELETE' }), 'Categoría eliminada')) reload();
    }
  };
  paint();
}

/* ---------- Movimientos ---------- */
async function movimientos() {
  let [prods, movs] = await Promise.all([api('/productos'), api('/movimientos')]);
  prods.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''));
  view.innerHTML = `
    ${head('Movimientos', 'Entradas y salidas de stock')}
    <div class="split">
      <form class="panel form-panel" id="mv">
        <h2>Registrar movimiento</h2>
        <div class="field"><label for="prod">Producto</label>
          <select id="prod" required><option value="">Selecciona un producto</option>
            ${prods.map(p => `<option value="${p.id}">${esc(p.codigo)} · ${esc(p.nombre)}</option>`).join('')}</select>
          <p class="hint" id="stockInfo">&nbsp;</p></div>
        <div class="seg" role="radiogroup" aria-label="Tipo de movimiento">
          <label><input type="radio" name="tipo" value="ENTRADA" checked><span>Entrada</span></label>
          <label><input type="radio" name="tipo" value="SALIDA"><span>Salida</span></label></div>
        <div class="field"><label for="cant">Cantidad</label><input id="cant" type="number" min="1" step="1" required></div>
        <button class="btn primary" type="submit">Registrar</button>
      </form>
      <section>
        <div class="toolbar">
          <select id="ftipo" aria-label="Filtrar por tipo"><option value="">Entradas y salidas</option><option value="ENTRADA">Solo entradas</option><option value="SALIDA">Solo salidas</option></select>
          <select id="fprod" aria-label="Filtrar por producto"><option value="">Todos los productos</option>${prods.map(p => `<option value="${p.id}">${esc(p.nombre)}</option>`).join('')}</select>
        </div>
        <div class="panel table-wrap"><table>
          <thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th class="num">Cantidad</th></tr></thead><tbody id="rows"></tbody></table></div>
      </section>
    </div>`;

  const paint = () => {
    const t = $('#ftipo').value, p = $('#fprod').value;
    const rows = movs.filter(m => (!t || m.tipo === t) && (!p || String(m.producto?.id) === p)).sort((a, b) => b.id - a.id);
    $('#rows').innerHTML = rows.length ? rows.map(m => `
      <tr><td>${fmtDate(m.fecha)}</td><td>${esc(m.producto?.nombre)}</td>
      <td class="${m.tipo === 'ENTRADA' ? 'mv-in' : 'mv-out'}">${m.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'}</td><td class="num">${m.cantidad}</td></tr>`).join('')
      : emptyRow(4, movs.length ? 'Sin resultados' : 'Sin movimientos', movs.length ? 'Prueba con otro filtro.' : 'Registra la primera entrada o salida.');
  };
  const prodSel = () => prods.find(p => p.id == $('#prod').value);
  const info = () => { const p = prodSel(); $('#stockInfo').textContent = p ? `Stock actual: ${p.stock} unidades` : '\u00a0'; };

  $('#prod').onchange = info;
  $('#ftipo').onchange = $('#fprod').onchange = paint;
  $('#mv').onsubmit = async e => {
    e.preventDefault();
    const p = prodSel(), tipo = $('input[name=tipo]:checked').value, cant = parseInt($('#cant').value, 10);
    if (!p || !(cant > 0)) return toast('Elige un producto y una cantidad mayor a 0.', 'error');
    if (tipo === 'SALIDA' && cant > p.stock) return toast(`Stock insuficiente: ${p.nombre} tiene ${p.stock} unidades.`, 'error');
    const ok = await guarded(() => api('/movimientos', { method: 'POST', body: { tipo, cantidad: cant, fecha: localISO(), producto: { id: p.id } } }),
      `${tipo === 'ENTRADA' ? 'Entrada' : 'Salida'} registrada`);
    if (ok) {
      [prods, movs] = await Promise.all([api('/productos'), api('/movimientos')]);
      prods.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''));
      $('#cant').value = ''; info(); paint();
    }
  };
  paint();
}

/* ---------- Usuarios ---------- */
const ROLES = ['ADMIN', 'ALMACENERO', 'CONSULTA'];
async function usuarios() {
  let lista = await api('/usuarios');
  view.innerHTML = `
    ${head('Usuarios', '', '<button class="btn primary" id="nuevo">Nuevo usuario</button>')}
    <div class="panel table-wrap"><table>
      <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th></th></tr></thead><tbody id="rows"></tbody></table></div>`;
  const paint = () => {
    $('#rows').innerHTML = lista.length ? lista.map(u => `
      <tr><td>${esc(u.nombre)}</td><td>${esc(u.correo)}</td><td>${esc(u.rol)}</td>
      <td><span class="pill ${u.activo ? 'ok' : 'off'}">${u.activo ? 'Activo' : 'Inactivo'}</span></td>
      <td class="row-actions"><button class="btn link" data-edit="${u.id}">Editar</button><button class="btn link del" data-del="${u.id}">Eliminar</button></td></tr>`).join('')
      : emptyRow(5, 'Aún no hay usuarios', 'Crea el primero con “Nuevo usuario”.');
  };
  const reload = async () => { lista = await api('/usuarios'); paint(); };
  async function form(u) {
    const roles = u && !ROLES.includes(u.rol) ? [u.rol, ...ROLES] : ROLES;
    const v = await openForm({
      title: u ? 'Editar usuario' : 'Nuevo usuario', values: u || { rol: 'CONSULTA', activo: true },
      fields: [
        { name: 'nombre', label: 'Nombre', required: true, maxlength: 100 },
        { name: 'correo', label: 'Correo', type: 'email', required: true, maxlength: 150 },
        { name: 'rol', label: 'Rol', type: 'select', required: true, options: roles.map(r => ({ value: r, label: r })) },
        { name: 'activo', label: 'Usuario activo', type: 'checkbox' }
      ]
    });
    if (!v) return;
    if (await guarded(() => api(u ? '/usuarios/' + u.id : '/usuarios', { method: u ? 'PUT' : 'POST', body: v }), u ? 'Usuario actualizado' : 'Usuario creado')) reload();
  }
  $('#nuevo').onclick = () => form();
  $('#rows').onclick = async e => {
    const ed = e.target.closest('[data-edit]'), del = e.target.closest('[data-del]');
    if (ed) form(lista.find(u => u.id == ed.dataset.edit));
    if (del) {
      const u = lista.find(x => x.id == del.dataset.del);
      const v = await openForm({ title: 'Eliminar usuario', message: `Se eliminará a ${u.nombre}.`, submitLabel: 'Eliminar', danger: true });
      if (v && await guarded(() => api('/usuarios/' + u.id, { method: 'DELETE' }), 'Usuario eliminado')) reload();
    }
  };
  paint();
}

/* ---------- Reportes ---------- */
const REPORTES = {
  bajo: {
    label: 'Stock bajo', url: '/reportes/stock-bajo', file: 'stock-bajo.csv', empty: 'Ningún producto está por debajo de ' + STOCK_MIN + ' unidades.',
    cols: [['Producto', p => p.nombre], ['Stock', p => p.stock, 'num'], ['Estado', p => p.estado || ''], ['Alerta', p => p.alerta || '']]
  },
  inv: {
    label: 'Inventario', url: '/reportes/inventario', file: 'inventario.csv', empty: 'No hay productos registrados.',
    cols: [['Código', p => p.codigo], ['Producto', p => p.nombre], ['Categoría', p => p.categoria?.nombre || ''],
      ['Precio', p => (p.precio ?? 0).toFixed(2), 'num'], ['Stock', p => p.stock, 'num'], ['Valor', p => ((p.precio || 0) * (p.stock || 0)).toFixed(2), 'num']]
  },
  mov: {
    label: 'Movimientos', url: '/reportes/movimientos', file: 'movimientos.csv', empty: 'No hay movimientos registrados.',
    cols: [['Fecha', m => fmtDate(m.fecha)], ['Producto', m => m.producto?.nombre || ''], ['Tipo', m => m.tipo], ['Cantidad', m => m.cantidad, 'num']]
  }
};

async function reportes() {
  let actual = 'bajo', datos = [];
  view.innerHTML = `
    ${head('Reportes', '', '<button class="btn" id="csv">Exportar CSV</button><button class="btn" id="print">Imprimir</button>')}
    <div class="tabs" role="tablist">${Object.entries(REPORTES).map(([k, r]) => `<button role="tab" data-k="${k}">${r.label}</button>`).join('')}</div>
    <div class="panel table-wrap" id="out"></div>`;

  async function load() {
    const r = REPORTES[actual];
    document.querySelectorAll('.tabs button').forEach(b => b.setAttribute('aria-selected', b.dataset.k === actual));
    $('#out').innerHTML = '<p class="loading">Cargando…</p>';
    try { datos = await api(r.url); } catch (e) { $('#out').innerHTML = `<div class="errbox">${esc(e.message)}</div>`; return; }
    if (actual === 'mov') datos = [...datos].sort((a, b) => b.id - a.id);
    $('#out').innerHTML = `<table><thead><tr>${r.cols.map(c => `<th class="${c[2] || ''}">${c[0]}</th>`).join('')}</tr></thead><tbody>
      ${datos.length ? datos.map(d => `<tr>${r.cols.map(c => `<td class="${c[2] || ''}">${esc(c[1](d))}</td>`).join('')}</tr>`).join('')
        : emptyRow(r.cols.length, 'Sin datos', r.empty)}</tbody></table>`;
  }
  $('.tabs').onclick = e => { const b = e.target.closest('button'); if (b) { actual = b.dataset.k; load(); } };
  $('#csv').onclick = () => { const r = REPORTES[actual]; downloadCSV(r.file, r.cols.map(c => c[0]), datos.map(d => r.cols.map(c => c[1](d)))); };
  $('#print').onclick = () => window.print();
  load();
}

/* ---------- Router ---------- */
const routes = { dashboard, productos, categorias, movimientos, usuarios, reportes };

async function route() {
  const name = location.hash.replace(/^#\//, '') || 'dashboard';
  const fn = routes[name] || dashboard;
  document.querySelectorAll('#nav a').forEach(a => a.classList.toggle('active', a.dataset.route === (routes[name] ? name : 'dashboard')));
  view.innerHTML = '<p class="loading">Cargando…</p>';
  try {
    await fn();
  } catch (e) {
    view.innerHTML = `<div class="errbox"><strong>No se pudo cargar la sección.</strong><br>${esc(e.message)}<br><button class="btn" id="retry">Reintentar</button></div>`;
    $('#retry').onclick = route;
  }
  view.focus({ preventScroll: true });
}
addEventListener('hashchange', route);
route();
