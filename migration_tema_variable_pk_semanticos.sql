-- ============================================================
-- Migración: da de alta en el catálogo de tema_variable los 12 tokens semánticos
-- --pk-success/-warning/-danger/-info (+ variantes -to/-soft) que ya usan ~53 pantallas
-- (badges de stock, botones agregar/quitar/activar/desactivar, cajas de error/aviso,
-- gradientes de botones destructivos o de éxito) pero que hasta ahora estaban fijos en
-- styles.scss, sin forma de editarlos desde Personalización.
--
-- El mecanismo ya es 100% genérico (ver TemaService.aplicarSegunTema() en el front): agregar
-- una fila nueva no requiere ningún cambio de código, ni aquí ni en el back -- exactamente el
-- mismo patrón que ya se usó para brand-*/card-*/sb-*/etc. en migration_tema_variable.sql.
--
-- Semilla: los valores *_claro y *_oscuro de abajo son EXACTAMENTE los que ya estaban a mano
-- en :root / :root[data-theme="dark"] (prefers-color-scheme: dark) de styles.scss, así que la
-- primera vez que se carga el catálogo no cambia nada visualmente -- el valor inyectado por
-- TemaService en document.body.style pisa al de la hoja de estilos con el mismo color exacto.
--
-- -to  = variante más oscura/saturada (texto sobre fondo -soft, o el segundo stop de un
--        gradient(-danger-to, -danger) en botones destructivos/de-éxito).
-- -soft = versión translúcida (rgba baja opacidad), usada como fondo de badges/cajas de aviso.
-- ============================================================

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden) VALUES
    -- Éxito (verde) -- badges "activo/entregado/aprobado", botones agregar/activar, montos positivos
    ('pk-success',      'Éxito — color principal',        'Estados', 'color', '#16a34a', '#34d399', 1),
    ('pk-success-to',   'Éxito — variante oscura/degradado', 'Estados', 'color', '#059669', '#10b981', 2),
    ('pk-success-soft', 'Éxito — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(22,163,74,0.10)', 'rgba(52,211,153,0.14)', 3),
    -- Advertencia (ámbar) -- badges "pendiente/apartado", avisos "revisa esto"
    ('pk-warning',      'Advertencia — color principal',        'Estados', 'color', '#f59e0b', '#fbbf24', 4),
    ('pk-warning-to',   'Advertencia — variante oscura/degradado', 'Estados', 'color', '#d97706', '#f59e0b', 5),
    ('pk-warning-soft', 'Advertencia — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(245,158,11,0.10)', 'rgba(251,191,36,0.14)', 6),
    -- Peligro/error (rojo) -- botones eliminar/cancelar/rechazar, cajas de error, montos negativos
    ('pk-danger',       'Peligro/Error — color principal',        'Estados', 'color', '#ef4444', '#f87171', 7),
    ('pk-danger-to',    'Peligro/Error — variante oscura/degradado', 'Estados', 'color', '#dc2626', '#ef4444', 8),
    ('pk-danger-soft',  'Peligro/Error — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(239,68,68,0.10)', 'rgba(248,113,113,0.14)', 9),
    -- Informativo (azul) -- badges "en curso/procesando", botones editar/compartir/transferir
    ('pk-info',         'Informativo — color principal',        'Estados', 'color', '#3b82f6', '#60a5fa', 10),
    ('pk-info-to',      'Informativo — variante oscura/degradado', 'Estados', 'color', '#2563eb', '#3b82f6', 11),
    ('pk-info-soft',    'Informativo — fondo suave (badges/cajas)', 'Estados', 'color', 'rgba(59,130,246,0.10)', 'rgba(96,165,250,0.14)', 12)
ON DUPLICATE KEY UPDATE clave = clave;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT grupo, clave, etiqueta, valor_claro, valor_oscuro FROM tema_variable WHERE grupo = 'Estados' ORDER BY orden;
