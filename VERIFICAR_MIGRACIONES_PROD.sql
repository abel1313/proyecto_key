-- =====================================================================================
-- VERIFICAR_MIGRACIONES_PROD.sql — SOLO LECTURA (puros SELECT; no crea, no cambia, no borra nada)
--
-- Qué hace: revisa que la base tenga las tablas, columnas, llaves y pantallas que el código de
-- `main` ya usa. Se corre en la base de prod (inventario_key) y se le pone el resultado al desarrollador.
--
-- Cómo correrlo (MySQL Workbench): abrir una pestaña SQL conectada a inventario_key, pegar TODO,
-- ejecutar. Salen 2 resultados:
--   1) RESUMEN: una fila con cuántos objetos están OK y cuántos FALTAN.
--   2) DETALLE: una fila por objeto; los que FALTAN salen primero.
-- Si FALTA algo, se corre la migración de la columna `migracion` (el número es el de la lista
-- «Pendiente para prod» de CLAUDE.md), EN ESE ORDEN, y después se vuelve a correr este script.
-- Antes de correr cualquier migración: respaldo de la base.
-- =====================================================================================

SELECT DATABASE() AS base_revisada,
       SUM(estado='OK') AS ok,
       SUM(estado='FALTA') AS faltan,
       COUNT(*) AS revisados
FROM (
  SELECT 1 AS n, '01 entrega_pedido' AS migracion, 'COLUMNA' AS tipo, 'pedidos.entregado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='entregado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 2 AS n, '01 entrega_pedido' AS migracion, 'COLUMNA' AS tipo, 'pedidos.fecha_entregado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='fecha_entregado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 3 AS n, '04 datos_legales' AS migracion, 'TABLA' AS tipo, 'datos_legales_negocio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='datos_legales_negocio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 4 AS n, '04 datos_legales' AS migracion, 'COLUMNA' AS tipo, 'usuario_modificacion.acepto_terminos' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario_modificacion' AND column_name='acepto_terminos')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 5 AS n, '04 datos_legales' AS migracion, 'COLUMNA' AS tipo, 'usuario_modificacion.fecha_acepto_terminos' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario_modificacion' AND column_name='fecha_acepto_terminos')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 6 AS n, '09 codigo_postal' AS migracion, 'TABLA' AS tipo, 'codigo_postal' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='codigo_postal')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 7 AS n, '10 sesion_refresh_gracia' AS migracion, 'COLUMNA' AS tipo, 'sesion_refresh.jti_anterior' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sesion_refresh' AND column_name='jti_anterior')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 8 AS n, '10 sesion_refresh_gracia' AS migracion, 'COLUMNA' AS tipo, 'sesion_refresh.rotado_en' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sesion_refresh' AND column_name='rotado_en')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 9 AS n, '11 submenu_corte_caja' AS migracion, 'PANTALLA' AS tipo, 'reportes/corte-caja' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='reportes/corte-caja')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 10 AS n, '12 calculo_precio' AS migracion, 'TABLA' AS tipo, 'producto_calculo_precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='producto_calculo_precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 11 AS n, '13 grupo_precio_cantidad' AS migracion, 'TABLA' AS tipo, 'grupo_precio_cantidad' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='grupo_precio_cantidad')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 12 AS n, '13 grupo_precio_cantidad' AS migracion, 'TABLA' AS tipo, 'grupo_precio_cantidad_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='grupo_precio_cantidad_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 13 AS n, '13 grupo_precio_cantidad' AS migracion, 'PANTALLA' AS tipo, 'productos/grupos-precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/grupos-precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 14 AS n, '14 precio_base_linea_grupo' AS migracion, 'TABLA' AS tipo, 'detalle_pedido_precio_grupo' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='detalle_pedido_precio_grupo')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 15 AS n, '14 precio_base_linea_grupo' AS migracion, 'COLUMNA' AS tipo, 'detalle_pedido_precio_grupo.origen' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='detalle_pedido_precio_grupo' AND column_name='origen')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 16 AS n, '15 campana' AS migracion, 'TABLA' AS tipo, 'campana' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 17 AS n, '15 campana' AS migracion, 'TABLA' AS tipo, 'campana_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 18 AS n, '15 campana' AS migracion, 'PANTALLA' AS tipo, 'productos/campanas' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/campanas')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 19 AS n, '16 campana_personas' AS migracion, 'TABLA' AS tipo, 'campana_persona' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_persona')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 20 AS n, '16 campana_personas' AS migracion, 'TABLA' AS tipo, 'campana_persona_pedido' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_persona_pedido')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 21 AS n, '17 campana_persona_cliente' AS migracion, 'COLUMNA' AS tipo, 'campana_persona.cliente_id' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='campana_persona' AND column_name='cliente_id')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 22 AS n, '17 campana_persona_cliente' AS migracion, 'COLUMNA' AS tipo, 'campana_persona.cliente_sin_registro_id' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='campana_persona' AND column_name='cliente_sin_registro_id')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 23 AS n, '17 campana_persona_cliente' AS migracion, 'LLAVE' AS tipo, 'campana_persona.fk_cper_cliente' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='campana_persona' AND constraint_name='fk_cper_cliente')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 24 AS n, '17 campana_persona_cliente' AS migracion, 'LLAVE' AS tipo, 'campana_persona.fk_cper_sin_registro' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='campana_persona' AND constraint_name='fk_cper_sin_registro')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 25 AS n, '18 reporte_categoria' AS migracion, 'TABLA' AS tipo, 'reporte_generado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='reporte_generado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 26 AS n, '19 tipo_precio_venta' AS migracion, 'COLUMNA' AS tipo, 'detalle_venta_variantes.tipo_precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='detalle_venta_variantes' AND column_name='tipo_precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 27 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 28 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote_gasto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote_gasto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 29 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 30 AS n, '20 flete_lote' AS migracion, 'PANTALLA' AS tipo, 'productos/fletes' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/fletes')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 31 AS n, '21 flete_venta' AS migracion, 'TABLA' AS tipo, 'venta_detalle_lote' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='venta_detalle_lote')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 32 AS n, '22 pedido_datos_visibles' AS migracion, 'COLUMNA' AS tipo, 'pedidos.datos_visibles' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='datos_visibles')>0 THEN 'OK' ELSE 'FALTA' END AS estado
) AS r;

SELECT migracion, tipo, objeto, estado
FROM (
  SELECT 1 AS n, '01 entrega_pedido' AS migracion, 'COLUMNA' AS tipo, 'pedidos.entregado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='entregado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 2 AS n, '01 entrega_pedido' AS migracion, 'COLUMNA' AS tipo, 'pedidos.fecha_entregado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='fecha_entregado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 3 AS n, '04 datos_legales' AS migracion, 'TABLA' AS tipo, 'datos_legales_negocio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='datos_legales_negocio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 4 AS n, '04 datos_legales' AS migracion, 'COLUMNA' AS tipo, 'usuario_modificacion.acepto_terminos' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario_modificacion' AND column_name='acepto_terminos')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 5 AS n, '04 datos_legales' AS migracion, 'COLUMNA' AS tipo, 'usuario_modificacion.fecha_acepto_terminos' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario_modificacion' AND column_name='fecha_acepto_terminos')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 6 AS n, '09 codigo_postal' AS migracion, 'TABLA' AS tipo, 'codigo_postal' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='codigo_postal')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 7 AS n, '10 sesion_refresh_gracia' AS migracion, 'COLUMNA' AS tipo, 'sesion_refresh.jti_anterior' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sesion_refresh' AND column_name='jti_anterior')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 8 AS n, '10 sesion_refresh_gracia' AS migracion, 'COLUMNA' AS tipo, 'sesion_refresh.rotado_en' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sesion_refresh' AND column_name='rotado_en')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 9 AS n, '11 submenu_corte_caja' AS migracion, 'PANTALLA' AS tipo, 'reportes/corte-caja' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='reportes/corte-caja')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 10 AS n, '12 calculo_precio' AS migracion, 'TABLA' AS tipo, 'producto_calculo_precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='producto_calculo_precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 11 AS n, '13 grupo_precio_cantidad' AS migracion, 'TABLA' AS tipo, 'grupo_precio_cantidad' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='grupo_precio_cantidad')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 12 AS n, '13 grupo_precio_cantidad' AS migracion, 'TABLA' AS tipo, 'grupo_precio_cantidad_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='grupo_precio_cantidad_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 13 AS n, '13 grupo_precio_cantidad' AS migracion, 'PANTALLA' AS tipo, 'productos/grupos-precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/grupos-precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 14 AS n, '14 precio_base_linea_grupo' AS migracion, 'TABLA' AS tipo, 'detalle_pedido_precio_grupo' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='detalle_pedido_precio_grupo')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 15 AS n, '14 precio_base_linea_grupo' AS migracion, 'COLUMNA' AS tipo, 'detalle_pedido_precio_grupo.origen' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='detalle_pedido_precio_grupo' AND column_name='origen')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 16 AS n, '15 campana' AS migracion, 'TABLA' AS tipo, 'campana' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 17 AS n, '15 campana' AS migracion, 'TABLA' AS tipo, 'campana_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 18 AS n, '15 campana' AS migracion, 'PANTALLA' AS tipo, 'productos/campanas' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/campanas')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 19 AS n, '16 campana_personas' AS migracion, 'TABLA' AS tipo, 'campana_persona' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_persona')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 20 AS n, '16 campana_personas' AS migracion, 'TABLA' AS tipo, 'campana_persona_pedido' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='campana_persona_pedido')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 21 AS n, '17 campana_persona_cliente' AS migracion, 'COLUMNA' AS tipo, 'campana_persona.cliente_id' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='campana_persona' AND column_name='cliente_id')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 22 AS n, '17 campana_persona_cliente' AS migracion, 'COLUMNA' AS tipo, 'campana_persona.cliente_sin_registro_id' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='campana_persona' AND column_name='cliente_sin_registro_id')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 23 AS n, '17 campana_persona_cliente' AS migracion, 'LLAVE' AS tipo, 'campana_persona.fk_cper_cliente' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='campana_persona' AND constraint_name='fk_cper_cliente')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 24 AS n, '17 campana_persona_cliente' AS migracion, 'LLAVE' AS tipo, 'campana_persona.fk_cper_sin_registro' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='campana_persona' AND constraint_name='fk_cper_sin_registro')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 25 AS n, '18 reporte_categoria' AS migracion, 'TABLA' AS tipo, 'reporte_generado' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='reporte_generado')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 26 AS n, '19 tipo_precio_venta' AS migracion, 'COLUMNA' AS tipo, 'detalle_venta_variantes.tipo_precio' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='detalle_venta_variantes' AND column_name='tipo_precio')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 27 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 28 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote_gasto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote_gasto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 29 AS n, '20 flete_lote' AS migracion, 'TABLA' AS tipo, 'compra_lote_producto' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='compra_lote_producto')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 30 AS n, '20 flete_lote' AS migracion, 'PANTALLA' AS tipo, 'productos/fletes' AS objeto, CASE WHEN (SELECT COUNT(*) FROM submenu WHERE ruta='productos/fletes')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 31 AS n, '21 flete_venta' AS migracion, 'TABLA' AS tipo, 'venta_detalle_lote' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='venta_detalle_lote')>0 THEN 'OK' ELSE 'FALTA' END AS estado
  UNION ALL
  SELECT 32 AS n, '22 pedido_datos_visibles' AS migracion, 'COLUMNA' AS tipo, 'pedidos.datos_visibles' AS objeto, CASE WHEN (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pedidos' AND column_name='datos_visibles')>0 THEN 'OK' ELSE 'FALTA' END AS estado
) AS r
ORDER BY (estado='OK'), n;
