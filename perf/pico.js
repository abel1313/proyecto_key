import http from 'k6/http';
import { check, sleep } from 'k6';

// La búsqueda sin resultados responde 404: no es un error del sistema.
http.setResponseCallback(http.expectedStatuses(200, 404));

// Pico tipo Buen Fin: sube de golpe a 150 usuarios, se queda 3 minutos y baja.
const BASE = __ENV.BASE;
export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 150 },
    { duration: '3m', target: 150 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.02', abortOnFail: true, delayAbortEval: '30s' }],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const res = http.get(`${BASE}/v1/variantes/buscar?termino=bolsa&pagina=1&size=12`);
  check(res, { 'buscar 200': (r) => r.status === 200 });
  sleep(1);
}
