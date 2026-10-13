// Mide si se lee lo que hay en pantalla, con los colores de un diseño, de día o de noche.
// Skill revision-colores. Uso (ver SKILL.md para levantar el front y sacar las filas):
//   node medir-contraste.js --filas filas-jade.json --hora 21:00 --salida ./res \
//        --rutas "/pedidos/mis-pedidos /tienda/buscar" [--texto] [--base http://127.0.0.1:18090]
// Mide: (1) el menú de opciones de cada <select> (como lo dibujaría el navegador), (2) el panel de
// cada p-dropdown (lo abre), (3) un <select> dentro de una ventana Swal, y con --texto (4) cada
// texto visible contra el fondo que tiene detrás. Escribe resultado.json y avisa todo lo < 4.5.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || '/opt/node22/lib/node_modules/playwright');
const fs = require('fs');

const args = {};
process.argv.slice(2).forEach((a, i, arr) => { if (a.startsWith('--')) args[a.slice(2)] = arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : true; });
const BASE = args.base || 'http://127.0.0.1:18090';
const API = args.api || 'http://localhost:9091/mis-productos';
const salida = args.salida || './resultado-contraste';
const hora = args.hora || '21:00';
const filas = args.filas ? JSON.parse(fs.readFileSync(args.filas, 'utf8')) : [];
const RUTAS = (args.rutas || '/pedidos/mis-pedidos').split(/\s+/).filter(Boolean);
const MINIMO = Number(args.minimo || 4.5);
fs.mkdirSync(salida, { recursive: true });

// Token falso de administrador con todas las pantallas que se van a abrir (base64 normal, no url).
const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64');
const pantallas = RUTAS.map(r => r.replace(/^\//, '').replace(/\/\d+$/, ''));
const token = b64({ alg: 'HS256' }) + '.' + b64({ sub: 'admin', idUsuario: 1, roles: ['ROLE_ADMIN'], pantallas,
  pantallasAcciones: [], pantallasEscritura: pantallas, exp: 4102444800 }) + '.firma';
const pagos = [{ id: 1, formaPago: 'Efectivo', mostrarMeses: false, opciones: [{ id: 1, descripcion: 'Contado' }] },
               { id: 2, formaPago: 'Tarjeta', mostrarMeses: true, opciones: [{ id: 2, descripcion: '3 meses' }] }];

// Funciones que corren dentro de la página (se pasan como texto).
const UTIL = `
  const parse = s => { const m = /rgba?\\(([^)]+)\\)/.exec(s || ''); if (!m) return null; const a = m[1].split(/[ ,\\/]+/).filter(Boolean).map(Number); return { r: a[0], g: a[1], b: a[2], a: a.length > 3 ? a[3] : 1 }; };
  const comp = (t, b) => ({ r: t.r * t.a + b.r * (1 - t.a), g: t.g * t.a + b.g * (1 - t.a), b: t.b * t.a + b.b * (1 - t.a), a: 1 });
  const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b); };
  const ratio = (x, y) => { const a = lum(x), b = lum(y); return +(((Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)).toFixed(2)); };
  const fondoReal = el => { let capas = []; for (let e = el; e; e = e.parentElement) { const c = parse(getComputedStyle(e).backgroundColor); if (c && c.a > 0) { capas.push(c); if (c.a >= 1) break; } }
    let base = { r: 255, g: 255, b: 255, a: 1 }; for (let i = capas.length - 1; i >= 0; i--) base = comp(capas[i], base); return base; };
`;
const medirSelects = new Function(`${UTIL}
  const out = [];
  document.querySelectorAll('select').forEach((s, i) => {
    const op = s.options[Math.min(1, s.options.length - 1)]; if (!op) return;
    const cs = getComputedStyle(s), oc = getComputedStyle(op);
    const esquema = cs.colorScheme.includes('dark') ? 'dark' : 'light';
    // Base del menú cuando el navegador lo pinta solo por color-scheme (Chrome: blanco / #3b3b3b).
    const baseMenu = esquema === 'dark' ? { r: 59, g: 59, b: 59, a: 1 } : { r: 255, g: 255, b: 255, a: 1 };
    const fg = parse(oc.color), bg = parse(oc.backgroundColor) || { r: 0, g: 0, b: 0, a: 0 };
    if (!fg) return;
    const bgA = comp(bg, baseMenu);
    const conFondo = ratio(fg.a < 1 ? comp(fg, bgA) : fg, bgA);              // respeta fondo y letra
    const sinFondo = ratio(fg.a < 1 ? comp(fg, baseMenu) : fg, baseMenu);     // solo letra + color-scheme
    const fc = fondoReal(s), lc = parse(cs.color) || fg;
    out.push({ tipo: 'select', que: (s.id || s.className || 'select#' + i).toString().slice(0, 40) + ' «' + op.text.trim().slice(0, 18) + '»',
      esquema, letra: oc.color, fondo: oc.backgroundColor, contraste: Math.min(conFondo, sinFondo), conFondo, sinFondo,
      campoCerrado: ratio(lc.a < 1 ? comp(lc, fc) : lc, fc) });
  });
  return out;`);
const medirPanel = new Function(`${UTIL}
  const panel = document.querySelector('.p-dropdown-panel'); if (!panel) return null;
  const item = panel.querySelector('.p-dropdown-item:not(.p-highlight)') || panel.querySelector('.p-dropdown-item'); if (!item) return null;
  const fg = parse(getComputedStyle(item).color), bg = fondoReal(item); if (!fg) return null;
  return { tipo: 'p-dropdown', que: item.textContent.trim().slice(0, 30), letra: getComputedStyle(item).color, fondo: getComputedStyle(panel).backgroundColor, contraste: ratio(fg, bg) };`);
const medirTexto = new Function(`${UTIL}
  const out = [], vistos = new Set();
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let n = w.nextNode(); n && out.length < 600; n = w.nextNode()) {
    const el = n.parentElement; if (!el || vistos.has(el) || !n.textContent.trim()) continue; vistos.add(el);
    if (el.closest('script,style,option,select,[aria-hidden="true"]')) continue;
    const r = el.getBoundingClientRect(), cs = getComputedStyle(el);
    if (r.width < 2 || r.height < 2 || cs.visibility === 'hidden' || +cs.opacity === 0) continue;
    // Fondo con imagen o degradado detrás: no se puede medir solo con colores, se marca para verlo en la captura.
    let conImagen = false; for (let e = el; e; e = e.parentElement) { const s = getComputedStyle(e); if (s.backgroundImage !== 'none') { conImagen = true; break; } if ((parse(s.backgroundColor) || { a: 0 }).a >= 1) break; }
    const fg = parse(cs.color), bg = fondoReal(el);
    if (!fg) continue; // color que no es rgb (p. ej. "transparent" o color(...)): no se mide
    const c = ratio(fg.a < 1 ? comp(fg, bg) : fg, bg);
    out.push({ tipo: 'texto', que: (el.className && el.className.toString ? el.className.toString() : el.tagName).slice(0, 40) + ' «' + n.textContent.trim().slice(0, 24) + '»', letra: cs.color, fondo: 'compuesto', contraste: c, conImagen });
  }
  return out;`);

(async () => {
  const b = await chromium.launch({ executablePath: process.env.CHROMIUM || '/opt/pw-browsers/chromium' });
  const ctx = await b.newContext({ viewport: { width: Number(args.ancho || 1360), height: 900 }, timezoneId: 'America/Mexico_City' });
  // La hora decide día o noche (ThemeService: de 19:00 a 6:00 es noche).
  await ctx.clock.install({ time: new Date(`2026-10-13T${hora}:00-06:00`) });
  await ctx.route(API.replace(/\/mis-productos$/, '') + '/**', async r => {
    const u = r.request().url();
    const cors = { 'Access-Control-Allow-Origin': new URL(BASE).origin, 'Access-Control-Allow-Credentials': 'true',
      'Access-Control-Allow-Headers': r.request().headers()['access-control-request-headers'] || '*', 'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,PATCH,OPTIONS' };
    if (r.request().method() === 'OPTIONS') return r.fulfill({ status: 204, headers: cors });
    const j = (o, s = 200) => r.fulfill({ status: s, headers: cors, contentType: 'application/json', body: JSON.stringify(o) });
    // El front lee el refresh de las dos formas (accessToken y response.accessToken).
    if (u.includes('/v1/auth/refresh')) return j({ accessToken: token, response: { accessToken: token } });
    if (u.includes('/v1/tema-variable/activo')) return j({ data: filas });
    if (u.includes('/v1/pagos/opciones-estructuradas')) return j({ data: pagos, code: 200 });
    if (u.includes('filtros-disponibles')) return j({ data: { tallas: ['CH', 'M'], colores: ['Negro'], marcas: ['Jade'], precioMin: 100, precioMax: 900 } });
    return j({ data: [], lista: [], response: [], code: 200 });
  });
  await ctx.addInitScript(() => { try { localStorage.setItem('tienda-buscar:filtros-abiertos', '1'); localStorage.setItem('productos-buscar:filtros-abiertos', '1'); } catch {} });
  const p = await ctx.newPage();
  const res = [];
  for (const ruta of RUTAS) {
    await p.goto(BASE + ruta, { waitUntil: 'domcontentloaded' });
    await p.clock.runFor(2500); await p.waitForTimeout(700);
    const nombre = ruta.replace(/\W+/g, '_');
    await p.screenshot({ path: `${salida}/${nombre}.png`, fullPage: true });
    (await p.evaluate(medirSelects)).forEach(x => res.push({ ruta, ...x }));
    if (args.texto) (await p.evaluate(medirTexto)).forEach(x => res.push({ ruta, ...x }));
    const dds = await p.$$('.p-dropdown');
    for (let k = 0; k < dds.length; k++) {
      try {
        await dds[k].click({ timeout: 2000 }); await p.waitForTimeout(400);
        const m = await p.evaluate(medirPanel); if (m) res.push({ ruta, ...m });
        await p.screenshot({ path: `${salida}/${nombre}-desplegable${k}.png` });
        await p.keyboard.press('Escape'); await p.waitForTimeout(200);
      } catch (e) { res.push({ ruta, tipo: 'p-dropdown', error: 'no se pudo abrir: ' + e.message.slice(0, 60) }); }
    }
  }
  // Un <select> dentro de una ventana Swal, con las clases que usa la app.
  await p.evaluate(() => { const d = document.createElement('div'); d.className = 'swal2-container swal2-center';
    d.innerHTML = '<div class="swal2-popup swal2-modal swal2-show" style="display:grid"><select class="swal2-select" style="display:flex"><option>Elige</option><option>Efectivo</option></select></div>';
    document.body.appendChild(d); });
  (await p.evaluate(medirSelects)).filter(x => /swal2/.test(x.que)).forEach(x => res.push({ ruta: 'ventana Swal', ...x }));
  fs.writeFileSync(`${salida}/resultado.json`, JSON.stringify(res, null, 1));
  const malos = res.filter(x => !x.error && x.contraste < MINIMO);
  console.log(`medidos=${res.filter(x => !x.error).length} con contraste < ${MINIMO}: ${malos.length}`);
  malos.slice(0, 40).forEach(x => console.log(`  ${x.contraste.toFixed(2)}  ${x.ruta}  ${x.tipo}  ${x.que}  letra=${x.letra} fondo=${x.fondo}${x.conImagen ? '  (hay imagen/degradado detrás: ver captura)' : ''}`));
  res.filter(x => x.error).forEach(x => console.log(`  ⚠ ${x.ruta} ${x.tipo}: ${x.error}`));
  await b.close();
})();
