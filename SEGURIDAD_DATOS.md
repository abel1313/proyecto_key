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
