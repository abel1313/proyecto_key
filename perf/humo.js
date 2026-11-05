import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE;
export const options = {
  vus: 1, duration: '1m',
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<1500'] },
};

export default function () {
  const rutas = ['/actuator/health', '/v1/negocio/estado', '/v1/cinta/activos', '/v1/promociones/activas'];
  for (const r of rutas) {
    const res = http.get(`${BASE}${r}`);
    check(res, { [`${r} responde 200`]: (x) => x.status === 200 });
  }
  sleep(1);
}
