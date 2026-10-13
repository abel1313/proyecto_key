// Skill revision-colores. Convierte los 5 diseños de Personalización (presets-diseno.ts, ya
// empaquetado con esbuild) en filas como las que regresa GET /v1/tema-variable/activo, para que
// medir-contraste.js las aplique igual que la app. También escribe filas-sin.json (sin filas =
// los colores de fábrica de styles.scss).
// Uso: node filas-de-presets.js /tmp/presets.cjs ./filas
const fs = require('fs');
const path = require('path');
const [modulo, salida] = [process.argv[2], process.argv[3] || './filas'];
const { PRESETS_DISENO } = require(path.resolve(modulo));
fs.mkdirSync(salida, { recursive: true });
// Solo importa para TemaService: "numero" se aplica con px; estilo y card-shadow se tratan por clave.
const tipo = clave => /^(estilo|card-shadow)$/.test(clave) ? 'seleccion'
  : /-(size|height|weight|radius)$/.test(clave) ? 'numero' : 'color';
for (const p of PRESETS_DISENO) {
  const filas = Object.entries(p.valores).map(([clave, v], i) => ({
    id: i + 1, clave, etiqueta: clave, tipo: tipo(clave), valorClaro: v.claro, valorOscuro: v.oscuro, orden: i + 1 }));
  fs.writeFileSync(path.join(salida, `filas-${p.id}.json`), JSON.stringify(filas));
  console.log(`filas-${p.id}.json  ${filas.length} filas  estilo=${(p.valores.estilo || {}).claro}/${(p.valores.estilo || {}).oscuro}`);
}
fs.writeFileSync(path.join(salida, 'filas-sin.json'), '[]');
console.log('filas-sin.json  (colores de fábrica)');
