# Pendientes para retomar (anotado 2026-11-04)

## En curso
- [ ] **Stock descuadre** ("No hay suficiente stock de 'Great Jeans talla 14 AZUL MARINO'. Disponible: 0"):
  1. Vender validando solo el stock del artículo (el del modelo baja pero no bloquea, nunca < 0).
  2. Alerta en Revisión de ventas: "modelo con menos stock que la suma de sus artículos".
  3. Prueba automática + actualizar R4 en `hexagonal/articulo/README.md`.
- [ ] **guardarConImagenes con miles de artículos** (QA, productoId 265, stock 5 cada uno, vacíos):
  - Hecho (sin commit): tope de 200 artículos por llamada en `VarianteServiceImpl.guardarConImagenes`.
  - Falta: saber qué pantalla/script los generó (alta-articulos limita por stock libre; parece una llamada manual o un bucle) y tope de `cantidad` en el front.
  - Falta: prueba automática del tope.

## Validado en QA por el dueño (2026-11-04)
- [x] Fotos y nombre en Tienda → Buscar, chat como cliente y como admin, tope de 200, alta de artículos, seguridad (getAll), escáner con celular.
- [ ] Carrito desde una tarjeta del chat (el back ya no manda `stock` al cliente; el front usa `disponible`): sin probar.
- [ ] **Venta con stock en cero** (Great Jeans talla 14): se deja así por ahora, sigue pendiente (tres puntos del descuadre, arriba).
- [ ] **Facebook (para después):** confirmar `FACEBOOK_APP_SECRET` en QA y prod ANTES de llevar a prod el cierre del webhook (falla cerrado: sin la variable, dejan de llegar los mensajes de Facebook).

## Incidente resuelto (QA, 2026-11-04): micro de imágenes caído
- Síntoma: Buscar con «Con imágenes» mostraba tarjetas sin foto; `.../imagenes/thumbnail/...` daba 502.
- Causa: el pod `imagenes-deployment` de QA estaba en CrashLoopBackOff (95 reinicios): RabbitMQ rechazaba el login (`ACCESS_REFUSED`). `SPRING_RABBITMQ_USERNAME` iba vacío (Spring usaba `guest`, que no existe; Rabbit solo tiene `admin`) y la contraseña del secreto `rabbit-imagenes` no coincidía con la de Rabbit. Probable origen: Rabbit se reinició al volver la VPS (OVH) y perdió/cambió credenciales.
- Arreglo: usuario `admin` en el deployment y contraseña igual en el secreto y en Rabbit; rollout restart. El back de QA (`proyecto-key-deployment`) también se cayó por la misma contraseña y se corrigió leyendo `SPRING_RABBITMQ_PASSWORD` del secreto `rabbit-imagenes`.
- [ ] Confirmar pod estable (0 reinicios) y que el back de QA sigue conectado a Rabbit.
- [ ] Dejar el usuario de Rabbit en el deployment como `value: admin` explícito (o en el secreto) para que no vuelva a quedar vacío.
- [ ] Revisar que Rabbit use almacenamiento persistente (si no, pierde usuarios al reiniciar).
- [ ] Alerta/aviso cuando un pod de QA/prod pase de N reinicios (este llevó 11 h caído sin que nadie lo viera).
- [ ] Borrar del historial de bash de la VPS las líneas con contraseñas.

## Seguridad
- [ ] Pasar a prod seguridad (back main + front master juntos, cherry-pick) — solo con orden explícita.
- [ ] Confirmar `FACEBOOK_APP_SECRET` en QA y prod.
- [ ] Verificar en BD que ROLE_USUARIO no tenga "Editar" en tienda/buscar.
- [ ] micro_imagenes: commit 97a7aee solo local; falta permiso de push.
- [ ] (Dueño) Rotar contraseñas MySQL/Rabbit, `git rm --cached application-dev.yml`, limpiar historial.
- [ ] CSP: pasar de Report-Only a bloqueo cuando la consola de QA esté limpia.
- [ ] Topes en GET públicos del micro de imágenes; subir Angular 14.

## Prod pendiente de ordenar
- [ ] Stock 1 por default (36a8e1e) y escáner/foto/MSI solo admin (80ae36d).
- [ ] Revisar pods de la VPS tras el regreso de OVH (`kubectl get pods -A`).

## Validaciones del dueño en QA
- [ ] Escáner y fotos con celular real.
- [ ] Bot con `chatbot.info-completa.roles` en QA; búsqueda "producto1".
