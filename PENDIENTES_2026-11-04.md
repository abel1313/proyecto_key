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
