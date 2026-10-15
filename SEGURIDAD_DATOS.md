# Seguridad de los datos de clientes — medidas y qué hacer si hay una fuga (2026-10-07)

La LFPDPPP (art. 18, ley de 2025) pide medidas de seguridad **administrativas, físicas y técnicas,
documentadas**, y avisar a los clientes si una vulneración les afecta de forma importante. Este es
ese documento. Sale de `LEGAL_PLAN_DE_ACCION.md`, punto 15.

## 1. Qué datos guardamos y dónde

| Datos | Dónde | Quién los ve |
|---|---|---|
| Cuentas (usuario, correo, contraseña **cifrada**), clientes (nombre, teléfono, correo, dirección de entrega), pedidos, abonos, conversaciones del chat y de redes | Base MySQL en el servidor de **OVHcloud** (Europa) | Personal con permiso de la pantalla (Gestión de roles) |
| Imágenes de productos | Disco del servidor (`micro_imagenes`) | Público (son del catálogo) |
| Textos del chat que contesta el bot | **OpenAI** (EE. UU.), hasta 30 días | Nadie del negocio fuera del sistema |
| Correos enviados (tickets, códigos) | **Gmail** (EE. UU.) | Quien tenga la cuenta de Gmail del negocio |
| Datos de tarjeta | **Mercado Pago** — nunca pasan por nuestro servidor | Nadie del negocio |

## 2. Medidas que ya existen

**Técnicas**
- Contraseñas cifradas (BCrypt); nadie del negocio puede verlas.
- Conexión cifrada (https) en la tienda.
- Encabezados de seguridad en la tienda (2026-10-07, ver §6): no se puede meter dentro de otra
  página (clickjacking), el navegador no adivina tipos de archivo, solo usa https una vez que entró
  por https, y la cámara y la ubicación solo las puede pedir la propia tienda.
- Sesión con token de 15 minutos y renovación de 7 días en cookie segura; cerrar sesión la invalida.
- Cambiar la contraseña invalida los tokens anteriores.
- Bloqueo por intentos fallidos de login y límite de registros por IP (en dev y prod).
- Permisos por pantalla y por acción (Gestión de roles): cada quien ve solo lo que necesita.
- No se guardan números de tarjeta.

**Administrativas**
- Aviso de privacidad publicado (`/privacidad`) y procedimiento ARCO (5 + 15 días hábiles).
- Página para pedir el borrado (`/eliminar-datos`).
- Llaves y contraseñas del servidor fuera del código (secretos de Kubernetes, ver `VPS_AUDITORIA.md`).

**Físicas**
- El servidor está en un centro de datos de OVHcloud (acceso físico controlado por OVH).
- Las computadoras y celulares con acceso al admin deben tener bloqueo de pantalla.

## 3. Reglas para el personal

1. **Nunca** pedir números de tarjeta por chat, WhatsApp, correo ni teléfono.
2. No compartir usuarios: cada persona con su cuenta y su rol.
3. No sacar listas de clientes del sistema (capturas, Excel) salvo que se necesiten para un trámite.
4. Si alguien deja de trabajar en el negocio, **desactivar su usuario el mismo día**.
5. No escribir datos de clientes en redes sociales ni en grupos de WhatsApp.

## 4. Si hay una fuga o un acceso indebido — paso a paso

| # | Qué hacer | Quién | Cuándo |
|---|---|---|---|
| 1 | **Cortar el acceso:** desactivar el usuario sospechoso, cambiar contraseñas del admin, del servidor y de Gmail; si fue una llave (OpenAI, Meta, Mercado Pago), **revocarla y generar otra** | Dueño + quien administre el servidor | De inmediato |
| 2 | **Revisar qué pasó:** qué datos, de cuántos clientes, desde cuándo. Guardar logs (`kubectl logs`), `historial_acceso` y capturas | Quien administre el servidor | Primeras 24 h |
| 3 | **Decidir si afecta de forma importante** a los clientes (contraseñas, teléfonos, direcciones, pedidos) | Dueño (con abogado si es grave) | 24–72 h |
| 4 | **Avisar a los clientes afectados** por correo: qué pasó, qué datos, qué hicimos y qué pueden hacer (cambiar contraseña, cuidarse de llamadas falsas). Sin culpas ni tecnicismos | Dueño | En cuanto se sepa el alcance |
| 5 | **Corregir la causa** y anotarla en `BITACORA_INCIDENTES.md` (fecha, qué pasó, qué se hizo) | Desarrollo | Después |

Plantilla del aviso:

> Asunto: Aviso importante sobre tu cuenta en Novedades Jade
>
> Hola, {nombre}. El {fecha} detectamos que {qué pasó, en una frase}. Pudieron quedar expuestos
> {qué datos}. Ya {qué hicimos}. Te recomendamos {cambiar tu contraseña / no contestar llamadas
> que pidan datos a nombre de la tienda}. Nosotros nunca te pediremos contraseñas ni datos de
> tarjeta. Si tienes dudas, escríbenos a {correo}.

## 5. Pendientes

- [ ] Aceptar el acuerdo de tratamiento de datos (DPA) de OpenAI desde su cuenta (`LEGAL_PLAN_DE_ACCION.md`, punto 23).
- [ ] Correo del negocio con dominio propio en lugar de Gmail personal (punto 25).
- [ ] Revisar cada 6 meses quién tiene acceso al admin y quitar a quien ya no lo necesite.

## 6. Revisión del 2026-10-07 — lo que salió al revisar lo legal

Al revisar los dos videos legales (`LEGAL_PLAN_DE_ACCION.md`, puntos 15 y 20) y el código nuevo de
datos legales salieron estos puntos de seguridad. Se prueban en la **Prueba 13** de
`GUIA_DE_PRUEBAS_QA.md`.

| # | Hallazgo | Riesgo | Estado |
|---|---|---|---|
| S1 | La tienda (nginx del contenedor del front, `default.conf`) **no mandaba ningún encabezado de seguridad**. El back sí los manda (Spring Security), la tienda no | Otra página podía mostrar la tienda dentro de un marco invisible y engañar al cliente para que tocara botones (clickjacking); sin HSTS, el primer acceso por `http://` se puede interceptar | ✅ **Corregido en `dev`** (front, `default.conf`): `X-Frame-Options: SAMEORIGIN`, `Content-Security-Policy: frame-ancestors 'self'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` (cámara y ubicación solo la tienda, micrófono nadie), `Strict-Transport-Security` de 1 año y `server_tokens off` (ya no dice la versión de nginx). Probado con nginx 1.24: la tienda, sus rutas y sus archivos responden 200 con los encabezados |
| S2 | ¿`http://` manda solo a `https://`? (punto 4 del video 1) | Si no redirige, alguien en la misma red puede ver lo que se manda | ⚠️ **No se pudo comprobar desde aquí** (la red de este entorno no entra a la tienda). Lo revisa el dueño en la VPS: `curl -sI http://shop.novedades-jade.com.mx/` y `curl -sI http://qa.shop.novedades-jade.com.mx/` → tiene que salir `301` con `Location: https://…`. Si sale `200`, en el bloque `server { listen 80; … }` de ese dominio poner `return 301 https://$host$request_uri;` (Certbot normalmente ya lo deja) |
| S3 | `GET /v1/datos-legales` es público y devuelve el **RFC**, y Términos lo muestra | El RFC de una persona física trae su **fecha de nacimiento**. La ley (LFPC 76 bis III) pide domicilio y teléfono antes de comprar, **no** el RFC; el RFC sí va en la factura | ❓ **Decisión del dueño, no se cambió**: el plan legal decidió mostrarlo. Opciones: (a) dejarlo; (b) quitarlo de la respuesta pública y de Términos y dejarlo solo para el ticket y la factura. Mientras no se capture, no se muestra nada |
| S4 | El límite de intentos de login y de registros por IP está **apagado en QA** (`seguridad.rate-limit-habilitado: false`, hallazgo 16 de `SEGURIDAD_AUTH.md`) | En QA, que está en internet, se pueden probar contraseñas sin límite | ⏭️ **Sigue así a propósito** (decisión del 2026-07-31). En dev y prod está encendido. Consecuencia: el límite de registros por IP **no se puede probar en QA** |
| S5 | No hay `Content-Security-Policy` completa (qué scripts e imágenes puede cargar la tienda) | Si alguna vez se cuela un script, el navegador no lo frena | ⏳ **Pendiente, a propósito**: una política completa sin probar rompe la tienda (Bootstrap, fuentes, imágenes del micro, el chat y el escáner). Se arma después con `Content-Security-Policy-Report-Only`, revisando la consola en QA, y luego se enciende |

