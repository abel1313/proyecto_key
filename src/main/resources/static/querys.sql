  CREATE TABLE permisos (
      id            INT AUTO_INCREMENT PRIMARY KEY,
      nombre_permiso VARCHAR(60) NOT NULL UNIQUE
  );

  CREATE TABLE rol_permiso (
      rol_id     INT NOT NULL,
      permiso_id INT NOT NULL,
      PRIMARY KEY (rol_id, permiso_id),
      FOREIGN KEY (rol_id)     REFERENCES roles(id),
      FOREIGN KEY (permiso_id) REFERENCES permisos(id)
  );

  CREATE TABLE usuario_permiso (
      usuario_id INT NOT NULL,
      permiso_id INT NOT NULL,
      PRIMARY KEY (usuario_id, permiso_id),
      FOREIGN KEY (usuario_id) REFERENCES usuario_modificacion(id),
      FOREIGN KEY (permiso_id) REFERENCES permisos(id)
  );

  -- ================================================
  -- 2. INSERTAR TODOS LOS PERMISOS
  -- ================================================

  INSERT INTO permisos (nombre_permiso) VALUES
  -- Productos
  ('PRODUCTOS_LEER'),
  ('PRODUCTOS_CREAR'),
  ('PRODUCTOS_EDITAR'),
  ('PRODUCTOS_ELIMINAR'),
  -- Variantes
  ('VARIANTES_LEER'),
  ('VARIANTES_CREAR'),
  ('VARIANTES_EDITAR'),
  -- Pedidos
  ('PEDIDOS_LEER'),
  ('PEDIDOS_CREAR'),
  ('PEDIDOS_EDITAR'),
  ('PEDIDOS_ELIMINAR'),
  -- Ventas
  ('VENTAS_LEER'),
  ('VENTAS_CREAR'),
  -- Clientes
  ('CLIENTES_LEER'),
  ('CLIENTES_CREAR'),
  ('CLIENTES_EDITAR'),
  ('CLIENTES_ELIMINAR'),
  -- MercadoPago
  ('MP_COBRAR'),
  -- Gastos
  ('GASTOS_GESTIONAR'),
  -- Rifas
  ('RIFAS_GESTIONAR'),
  -- Usuarios
  ('USUARIOS_GESTIONAR'),
  -- Imágenes
  ('IMAGENES_GESTIONAR'),
  -- Pagos catálogo
  ('PAGOS_LEER');

  -- ================================================
  -- 3. INSERTAR ROLES NUEVOS (ADMIN y USUARIO ya existen)
  -- ================================================

  INSERT INTO roles (nombre_rol) VALUES ('ROLE_EMPLEADO');
  INSERT INTO roles (nombre_rol) VALUES ('ROLE_CAJERO');

  -- ================================================
  -- 4. ASIGNAR PERMISOS A CADA ROL
  -- (usamos subqueries para no depender de IDs fijos)
  -- ================================================

  -- ── ROLE_ADMIN: todos los permisos ──────────────
  INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id
  FROM roles r, permisos p
  WHERE r.nombre_rol = 'ROLE_ADMIN';

  -- ── ROLE_EMPLEADO ────────────────────────────────
  INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id
  FROM roles r
  JOIN permisos p ON p.nombre_permiso IN (
      'PRODUCTOS_LEER','PRODUCTOS_CREAR','PRODUCTOS_EDITAR',
      'VARIANTES_LEER','VARIANTES_CREAR','VARIANTES_EDITAR',
      'PEDIDOS_LEER','PEDIDOS_CREAR','PEDIDOS_EDITAR','PEDIDOS_ELIMINAR',
      'VENTAS_LEER','VENTAS_CREAR',
      'CLIENTES_LEER','CLIENTES_CREAR','CLIENTES_EDITAR',
      'MP_COBRAR',
      'IMAGENES_GESTIONAR',
      'PAGOS_LEER'
  )
  WHERE r.nombre_rol = 'ROLE_EMPLEADO';

  -- ── ROLE_CAJERO ──────────────────────────────────
  INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id
  FROM roles r
  JOIN permisos p ON p.nombre_permiso IN (
      'PRODUCTOS_LEER',
      'PEDIDOS_LEER',
      'MP_COBRAR',
      'PAGOS_LEER'
  )
  WHERE r.nombre_rol = 'ROLE_CAJERO';

  -- ── ROLE_USUARIO ─────────────────────────────────
  INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id
  FROM roles r
  JOIN permisos p ON p.nombre_permiso IN (
      'PRODUCTOS_LEER',
      'PEDIDOS_LEER',
      'PEDIDOS_CREAR'
  )
  WHERE r.nombre_rol = 'ROLE_USUARIO';

  -- ================================================
  -- 5. VERIFICAR
  -- ================================================
  SELECT r.nombre_rol, p.nombre_permiso
  FROM roles r
  JOIN rol_permiso rp ON r.id = rp.rol_id
  JOIN permisos p     ON p.id = rp.permiso_id
  ORDER BY r.nombre_rol, p.nombre_permiso;



  DDL y DML — ejecutar en la base de datos

  -- ─────────────────────────────────────────────
  -- 1. Configuración del negocio (siempre 1 fila)
  -- ─────────────────────────────────────────────
  CREATE TABLE configuracion_negocio (
      id              INT             NOT NULL AUTO_INCREMENT,
      abierto         TINYINT(1)      NOT NULL DEFAULT 0,
      abierto_desde   DATETIME        NULL,
      cerrado_desde   DATETIME        NULL,
      hora_auto_cierre TIME           NOT NULL DEFAULT '21:00:00',
      whatsapp_url    VARCHAR(500)    NULL,
      facebook_url    VARCHAR(500)    NULL,
      actualizado_en  DATETIME        NULL,
      PRIMARY KEY (id)
  ) ENGINE=InnoDB;

  -- Fila inicial (id=1 fijo, negocio cerrado por default)
  INSERT INTO configuracion_negocio (id, abierto, hora_auto_cierre)
  VALUES (1, 0, '21:00:00');

  -- Migración: reemplazar hora_auto_cierre (TIME) por hora_apertura y hora_cierre (DATETIME)
  ALTER TABLE configuracion_negocio
      ADD COLUMN hora_apertura DATETIME NULL AFTER cerrado_desde,
      ADD COLUMN hora_cierre   DATETIME NULL AFTER hora_apertura,
      DROP COLUMN hora_auto_cierre;

  -- Poblar hora_cierre con el valor previo de hora_auto_cierre (si ya existía dato)
  -- UPDATE configuracion_negocio SET hora_cierre = CONCAT(CURDATE(), ' 21:00:00') WHERE id = 1;

  -- ─────────────────────────────────────────────
  -- 2. Imágenes de presentación (Login / Registro)
  -- ─────────────────────────────────────────────
  CREATE TABLE imagen_presentacion (
      id              INT             NOT NULL AUTO_INCREMENT,
      tipo            VARCHAR(20)     NOT NULL  COMMENT 'LOGIN o REGISTRO',
      orden           INT             NOT NULL  COMMENT '1, 2 o 3',
      url_imagen      VARCHAR(500)    NOT NULL  DEFAULT '',
      descripcion     VARCHAR(300)    NULL,
      activo          TINYINT(1)      NOT NULL  DEFAULT 1,
      actualizado_en  DATETIME        NULL,
      PRIMARY KEY (id),
      UNIQUE KEY uq_tipo_orden (tipo, orden)
  ) ENGINE=InnoDB;

  -- 6 filas iniciales: 3 para login, 3 para registro (vacías, las llenas desde el admin)
  INSERT INTO imagen_presentacion (tipo, orden, url_imagen, descripcion, activo) VALUES
  ('LOGIN',    1, '', '', 1),
  ('LOGIN',    2, '', '', 1),
  ('LOGIN',    3, '', '', 1),
  ('REGISTRO', 1, '', '', 1),
  ('REGISTRO', 2, '', '', 1),
  ('REGISTRO', 3, '', '', 1);

  -- Migración: agregar columnas para almacenamiento en disco (url_imagen pasa a guardar nombre de archivo UUID)
  ALTER TABLE imagen_presentacion
      ADD COLUMN extension      VARCHAR(10)  NULL AFTER url_imagen,
      ADD COLUMN nombre_original VARCHAR(200) NULL AFTER extension;
