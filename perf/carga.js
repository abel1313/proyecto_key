import http from 'k6/http';
import { check, sleep } from 'k6';

// La búsqueda sin resultados responde 404: no es un error del sistema.
http.setResponseCallback(http.expectedStatuses(200, 404));

// Carga normal: lo que hace una clienta que entra a ver la tienda (solo lectura pública).
const BASE = __ENV.BASE;
export const options = {
  stages: [
    { duration: '2m', target: 10 },
    { duration: '6m', target: 30 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

// Términos y páginas variados: con pocos términos la prueba mide la caché de Redis y no la base de datos.
const busquedas = ['bolsa', 'falda', 'pantalon', 'vestido', 'blusa', 'mochila', 'cartera', 'short', 'chamarra', 'sandalia', 'cinturon', 'bufanda'];

export default function () {
  const q = busquedas[Math.floor(Math.random() * busquedas.length)];
  const lista = http.get(`${BASE}/v1/variantes/buscar?termino=${q}&pagina=${1 + Math.floor(Math.random() * 3)}&size=12`);
  check(lista, { 'buscar 200 o sin resultados': (r) => r.status === 200 || r.status === 404 });
  sleep(Math.random() * 2 + 1);

  const promos = http.get(`${BASE}/v1/promociones/activas`);
  check(promos, { 'promos 200': (r) => r.status === 200 });
  sleep(Math.random() * 2 + 1);
}
