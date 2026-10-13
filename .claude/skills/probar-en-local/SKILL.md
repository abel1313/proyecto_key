---
name: probar-en-local
description: Cómo comprobar un cambio de Novedades Jade de punta a punta dentro del contenedor, sin tocar QA ni prod — base MySQL desechable, Redis local, el back completo con una prueba temporal de Spring, y el front compilado con la API simulada en Playwright (día/noche con la hora, token falso de admin). Usar antes de decirle al dueño "ya quedó" en algo que toca pedidos, cobros, migraciones, Preparar prueba, o cualquier pantalla; y cuando QA no es alcanzable desde el contenedor (el proxy bloquea qa.backend…).
---

# Probar en local — comprobar de verdad antes de subir

**Por qué:** desde el contenedor **no se llega a QA** (el proxy responde 403 a `qa.backend.novedades-jade.com.mx`).
Un "compila" no dice si el flujo funciona. Con esto se corre el código real, con una base y una pantalla
de verdad, y se ve el resultado antes de pedirle al dueño que pruebe.

Lo que **no** se hace aquí: commit de nada de esto. Las pruebas temporales se borran (regla del
proyecto: no se agregan tests automáticos) y las bases desechables viven en el scratchpad.

---

## 1. MySQL desechable

```bash
D=<scratchpad>/mysql_local; mkdir -p $D/data
mysqld --initialize-insecure --user=root --datadir=$D/data      # solo la primera vez
mysqld --user=root --datadir=$D/data --socket=$D/m.sock --port=33099 --mysqlx=OFF \
       --secure-file-priv= --pid-file=$D/m.pid &                 # si el contenedor se reinicia, otra vez
mysql -uroot -S $D/m.sock -e "SET GLOBAL sql_mode=''; CREATE DATABASE IF NOT EXISTS pk_preparar CHARACTER SET utf8mb4;"
```
- `sql_mode=''` porque Hibernate crea columnas que en modo estricto truenan con datos viejos.
- Para probar una **migración** con "safe updates" como en MySQL Workbench:
  `mysql --safe-updates -uroot -S $D/m.sock base < migracion.sql`, **dos veces** (la segunda no debe cambiar nada).
- `pkill -f mysqld` mata también el shell que lo lanzó (exit 144): usar `kill $(cat $D/m.pid)`.

## 2. Redis local

`CacheService.evictAll()` usa Redis; sin él, cualquier guardado de pedido truena con
*"Unable to connect to Redis"*.
```bash
redis-server --port 6379 --daemonize yes --dir <scratchpad> --save "" --appendonly no
redis-cli shutdown nosave     # al terminar
```

## 3. El back completo con una prueba temporal

Plantilla: `PruebaTemporalSpring.java.txt` (en esta carpeta). Se copia a
`src/test/java/com/ventas/key/tmpverif/TmpVerifTest.java`, se corre y **se borra la carpeta**.

```bash
mvn -q -o test -Dtest=TmpVerifTest -Dsurefire.failIfNoSpecifiedTests=false > salida.log 2>&1
grep -E "PEDIDOS|Tests run|<<< ERROR" salida.log;  grep -n "at com.ventas" salida.log | head
rm -rf src/test/java/com/ventas/key/tmpverif && git status --short | grep tmpverif   # debe salir vacío
```

Lo que ya se resolvió (no volver a investigar):

| Error al arrancar | Causa | Arreglo |
|---|---|---|
| *Unable to find a @SpringBootConfiguration* | El test no está bajo `com.ventas.key.mis.productos` | `@SpringBootTest(classes = MisProductosApplication.class)` |
| *mailHealthContributor: Beans must not be empty* | `@MockBean JavaMailSender` | `management.health.mail.enabled=false` |
| *Unable to connect to Redis* | Sin Redis | Sección 2 |
| *Catálogo de pago no encontrado: 1* | Base vacía sin `pagos_y_meses` | Seed de abajo |
| *Usuario no encontrado al crear venta* | No hay usuario 1 | Seed de abajo |
| *Failed to add the foreign key… usuario_modificacion* | Aviso de `ddl-auto=update` | Se ignora; no detiene el arranque |

**Seed mínimo** (después del primer arranque, cuando Hibernate ya creó las tablas):
```sql
INSERT IGNORE INTO tipo_pago(id, forma_pago) VALUES (1,'Efectivo'),(2,'Transferencia'),(3,'Tarjeta');
INSERT IGNORE INTO meses_intereses(id, descripcion, meses) VALUES (1,'Contado','0');
INSERT IGNORE INTO pagos_y_meses(id, meses_intereses_id, tipo_pago_id) VALUES (1,1,1),(2,1,2),(3,1,3),(62,1,2);
INSERT IGNORE INTO usuario_modificacion (id, username, password, enabled, correo_verificado,
       intentos_codigo_reset, intentos_codigo_verificacion) VALUES (1,'admin','x',1,1,0,0);
```
(`AbonoServiceImpl` usa los ids 1 = efectivo, 62 = transferencia, 2 = tarjeta.)

Correr la suite completa antes de entregar: `mvn -o test`. Las fallas que ya estaban están en
`TESTS_PENDIENTES.md`; si sale una que no está ahí, es nueva y hay que verla.

## 4. El front con la API simulada (Playwright)

```bash
npx ng build --configuration development                         # repo del front
setsid node .claude/skills/probar-en-local/servidor-estatico.js <front>/dist 18090 &
```
En el script de Playwright (ejemplo completo: `revision-colores/medir-contraste.js`):
- `require('/opt/node22/lib/node_modules/playwright')` y `executablePath: '/opt/pw-browsers/chromium'`.
  **Nunca** `playwright install`.
- **API simulada:** `ctx.route('http://localhost:9091/**', …)` con encabezados **CORS** (origen
  `http://127.0.0.1:18090`, credenciales) y respuesta 204 a `OPTIONS`. Sin CORS el front manda al login.
- **Token falso de admin:** JWT con `base64` **normal** (no base64url: el front usa `atob`), con `roles`,
  `pantallas`, `pantallasAcciones` y `pantallasEscritura` de las pantallas que se abren.
- **`/v1/auth/refresh`** responde las dos formas: `{ accessToken, response: { accessToken } }`.
- **Personalización:** `/v1/tema-variable/activo` → `{ data: filas }` (filas de `revision-colores`).
- **Día o noche:** `ctx.clock.install({ time: new Date('2026-10-13T21:00:00-06:00') })` (noche de 19 a 6).
  Después de navegar, `page.clock.runFor(2500)` para que corran los temporizadores.
- **Filtros abiertos:** `addInitScript` con `localStorage['tienda-buscar:filtros-abiertos'] = '1'`.
- **Sin sesión** (Login, Registro): `/v1/auth/refresh` responde 401.
- **Medir, no mirar:** `getComputedStyle` (colores, contraste) y capturas de lo que se mide.

## 5. Checklist

- [ ] Lo probé con el código real (prueba temporal o Playwright), no solo compiló.
- [ ] Borré `src/test/java/com/ventas/key/tmpverif` y no quedó nada temporal en `git status`.
- [ ] `mvn -o test` completo y `ng build` sin errores; las fallas conocidas son las de `TESTS_PENDIENTES.md`.
- [ ] Apagué Redis (y MySQL si ya no se usa).
