# Pruebas de carga con k6 (solo contra QA)

**Nunca contra producción.** Se corren desde una computadora aparte (no desde el VPS) apuntando a QA, con datos de prueba.

## Instalar k6
Windows: `winget install k6` · Mac: `brew install k6` · Linux: ver https://k6.io/docs/get-started/installation/

## Correr
```bash
# 1. Humo (1 minuto, 1 usuario): ¿responde todo?
k6 run -e BASE=https://qa.backend.novedades-jade.com.mx/mis-productos perf/humo.js

# 2. Carga normal (10 min, hasta 30 usuarios a la vez): ¿aguanta un día común?
k6 run -e BASE=... perf/carga.js

# 3. Pico (simula el Buen Fin: sube de golpe a 150 usuarios)
k6 run -e BASE=... perf/pico.js
```
Los umbrales (`thresholds`) hacen que la prueba **falle sola** si el 95 % de las peticiones tarda más de 800 ms o si hay más de 1 % de errores. Son valores comunes para una tienda pequeña, **no un estándar**: calíbralos con una primera corrida de línea base en QA.

## Qué mirar mientras corre
`kubectl top pods -n <namespace>` (CPU y memoria), el log del back y `SHOW PROCESSLIST;` en MySQL.

## Antes de la primera corrida
- Confirmar si QA comparte VPS con producción. Si lo comparte, correr de madrugada y con pocos usuarios.
- Las rutas de `carga.js` son de lectura pública. No toca ventas, pedidos ni cobros.
