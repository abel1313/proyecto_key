-- ============================================================
-- RIFAS — DDL COMPLETO DESDE CERO
-- IMPORTANTE: ejecutar cada sentencia por separado si el cliente
-- no soporta múltiples sentencias en una sola ejecución.
-- ============================================================


-- ------------------------------------------------------------
-- PASO 1 — Desactivar FK
-- ------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;


-- ------------------------------------------------------------
-- PASO 2 — Eliminar tablas (ejecutar una por una)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS ganador_rifa;

DROP TABLE IF EXISTS historial_rifa_variante;

DROP TABLE IF EXISTS concursantes;

DROP TABLE IF EXISTS rifas;

DROP TABLE IF EXISTS configurar_rifa_producto;

DROP TABLE IF EXISTS configurar_rifa_variante;

DROP TABLE IF EXISTS configurar_rifa;


-- ------------------------------------------------------------
-- PASO 3 — Reactivar FK
-- ------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 1;


-- ------------------------------------------------------------
-- PASO 4 — Crear tablas (ejecutar una por una)
-- ------------------------------------------------------------

-- Sesión de rifa (una sesión puede tener N variantes)
CREATE TABLE configurar_rifa (
    id                INT PRIMARY KEY AUTO_INCREMENT,
    fecha_hora_limite DATETIME   NOT NULL,
    activa            TINYINT(1) NOT NULL DEFAULT 1
);

-- Variantes que se rifan dentro de una sesión.
-- Al agregar: se descuenta 1 del stock de la variante.
-- Al eliminar: se devuelve ese stock.
CREATE TABLE configurar_rifa_variante (
    id                  INT         PRIMARY KEY AUTO_INCREMENT,
    configurar_rifa_id  INT         NOT NULL,
    variante_id         INT         NOT NULL,
    palabra_clave       VARCHAR(50) NOT NULL,
    giro_ganador        INT         NOT NULL DEFAULT 1,
    orden               INT         NOT NULL,
    permitir_nuevos     TINYINT(1)  NOT NULL DEFAULT 0,
    stock_reservado     INT         NOT NULL DEFAULT 1,
    UNIQUE KEY uq_rifa_palabra (configurar_rifa_id, palabra_clave),
    FOREIGN KEY (configurar_rifa_id) REFERENCES configurar_rifa(id),
    FOREIGN KEY (variante_id)        REFERENCES variantes(id)
);

-- Participantes de la rifa (solo el admin los registra).
-- palabra_clave liga al participante con una variante de la rifa.
-- cliente_pedido_id se usa cuando viene de la rifa mensual (pedidos).
CREATE TABLE concursantes (
    id                  INT          PRIMARY KEY AUTO_INCREMENT,
    nombre              VARCHAR(255) NOT NULL,
    apellido_paterno    VARCHAR(255),
    telefono            VARCHAR(50),
    descartado          TINYINT(1)   NOT NULL DEFAULT 0,
    orden_desde         INT          NOT NULL DEFAULT 1,
    palabra_clave       VARCHAR(50),
    cliente_pedido_id   INT          NULL,
    configurar_rifa_id  INT          NOT NULL,
    FOREIGN KEY (configurar_rifa_id) REFERENCES configurar_rifa(id)
);

-- Registro de cada giro del sorteo: descartado=1 eliminado, descartado=0 ganador.
CREATE TABLE ganador_rifa (
    id                          INT        PRIMARY KEY AUTO_INCREMENT,
    concursante_id              INT        NOT NULL,
    configurar_rifa_variante_id INT        NOT NULL,
    descartado                  TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (concursante_id)              REFERENCES concursantes(id),
    FOREIGN KEY (configurar_rifa_variante_id) REFERENCES configurar_rifa_variante(id)
);

-- Historial: resumen de cada variante sorteada con ganador y modo de continuación.
CREATE TABLE historial_rifa_variante (
    id                          INT        PRIMARY KEY AUTO_INCREMENT,
    configurar_rifa_id          INT        NOT NULL,
    configurar_rifa_variante_id INT        NOT NULL,
    concursante_ganador_id      INT        NULL,
    orden                       INT        NOT NULL,
    modo_continuacion           ENUM('RESTANTES','CERO','NUEVOS') NULL,
    FOREIGN KEY (configurar_rifa_id)          REFERENCES configurar_rifa(id),
    FOREIGN KEY (configurar_rifa_variante_id) REFERENCES configurar_rifa_variante(id),
    FOREIGN KEY (concursante_ganador_id)      REFERENCES concursantes(id)
);



 me ayudas con un script con datos dumis para algunos productos con varios stock sin imagenes y de esos productos varias variantes de cada uno igual sin imagenes, el objetivo es que igual me hagas un script para insertar rifas
   ya con participantes aleatorios pero varias rifas para retomar script con personas aleatorias unas 10 participantes con 2 o 3 variantes para rifar con palabras con fecha de hoy o mana solo para retomar la rifa el objetivo es
   siemrpe retomar, alguna duda?