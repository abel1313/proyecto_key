# Plan de paso a producción — 2026-10-15

## Hechos verificados (con GitHub, no de memoria)
| | Producción | QA | Diferencia |
|---|---|---|---|
| Back | `main` = `282a530` | `qa` = `abaafb3` | **177 commits** por subir |
| Front | `master` = `161c0d4f` | `qa` = `ea3754e0` | **156 commits** por subir |

- **Conflictos al unir: 0** en los dos repos (`git merge-tree`).
- **Producción no tiene nada que QA no tenga:** `main` y `master` están totalmente contenidos en `qa`. Todo hotfix que se subió a prod ya está en QA.
- **Redes sociales YA está en `main`** (webhooks, bot, dominio `botredes`: 76 archivos). No hay nada que excluir. La diferencia con QA en redes es pequeña: 3 archivos Java (manejo de errores de TikTok y un aviso en el log al arrancar que dice qué variables de redes faltan; nunca escribe valores).
- **Configuración:** `application.yml` cambia **una línea de comentario**. Sin variables ni propiedades nuevas.
- **Nota:** en un cálculo anterior se dijo "135 y 79 commits"; eran referencias locales desactualizadas. Los números buenos son los de la tabla.

## Qué ya está hecho en la base de producción
Respaldo manual `pre_migracion` (86 KB, íntegro) y SQL **1 a 7** corridos y verificados:
datos legales, sesión (jti_anterior/rotado_en), entrega de pedidos, código postal, permisos de gastos,
«Ver todos los modelos» y el texto «Dirección».
Pendientes: **8, 9 y 10** (diseño Jade), que se corren **justo antes de desplegar el front**.

## Orden del paso
1. **Respaldo fresco** de `inventario_key` (el `pre_migracion` + uno nuevo si pasó tiempo).
2. **Back:** unir `qa` → `main` (merge `--no-ff`) y subir. Dispara el deploy de prod (`kubectl rollout restart` en `default`). Esperar a que termine.
3. **Comprobar el back** en prod: login, Mis pedidos, un código postal, Gastos (ver abajo).
4. **SQL 8, 9 y 10** (diseño Jade) en `inventario_key`, en ese orden.
5. **Front:** unir `qa` → `master` y subir. Esperar su deploy.
6. **Todos vuelven a entrar** (los permisos viajan en el token; además la ventana de gracia cambia la sesión).
7. **Prueba de humo** en prod (lista de abajo).

## Prueba de humo en producción (10 minutos)
- Login con el admin y con un cliente.
- Tienda → Buscar: filtros de talla/color/marca, imágenes visibles.
- Mis pedidos: aparecen Entregado / Falta entregar; abrir un detalle.
- Gastos: se ve ➕ Agregar gasto.
- Mis datos: guardar un código postal.
- Configuración del negocio → Datos legales.
- Registro de un usuario nuevo (Términos).
- `/qr`.
- El chat 💬 de la tienda contesta sin datos técnicos.

## Si algo sale mal
| Qué | Cómo regresar |
|---|---|
| Back con errores | `kubectl rollout undo deployment proyecto-key-deployment -n default` (vuelve a la imagen anterior en ~1 min). Las columnas nuevas de la base son aditivas: el back viejo las ignora, no hay que deshacerlas. |
| Diseño Jade no gusta | Restaurar `tema_variable` desde `tema_variable_bkp_20261001` (el script 8 trae el SQL, comentado al final). |
| Front con errores | Revertir el merge en `master` y subir, o `kubectl rollout undo` del front. |
| Desastre de datos | Restaurar `pre_migracion/inventario_key_*.sql.gz` (**sin** `DROP DATABASE`: restaura sobre la base real). |

## Riesgos conocidos
1. **Todos los usuarios tendrán que volver a iniciar sesión.**
2. **El bot de la tienda cambia de comportamiento** (no da datos técnicos, pagos solo efectivo/transferencia, no da stock exacto).
3. **Jade cambia cómo se ve toda la app** (por diseño).
4. **Pendiente P1:** filtro de seguridad extra para el chat de la tienda y el chat en vivo. Decisión tuya: antes o después.
5. **Los respaldos** del cron guardan solo 3 días y están en el mismo disco (mejorar después, sin urgencia).
