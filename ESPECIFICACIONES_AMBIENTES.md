# Especificaciones de los ambientes

Con qué versiones y reglas corre cada ambiente. **Claude lo lee antes de escribir cualquier script
SQL, migración o prueba automática.** Si un dato dice ⏳ pendiente, no se supone: se le pide al
usuario con la consulta de la sección "Cómo obtener los datos de la base".

Última actualización: 2026-09-29

---

## 1. Base de datos

| Dato | QA (`inventario_key_qa`) | Prod (`inventario_key`) |
|---|---|---|
| Versión de MySQL | ⏳ pendiente | ⏳ pendiente |
| `sql_mode` | ⏳ pendiente | ⏳ pendiente |
| Charset / collation de la base | ⏳ pendiente | ⏳ pendiente |
| `lower_case_table_names` | ⏳ pendiente | ⏳ pendiente |
| Zona horaria | ⏳ pendiente | ⏳ pendiente |
| Esquema real exportado (sin datos) | ⏳ pendiente (`esquema_real_qa.sql`) | no hace falta si es igual a QA |

`dev` y `qa` usan la misma base (`inventario_key_qa`); `main` usa `inventario_key`. Ver CLAUDE.md,
"Mapeo rama → base de datos".

### Cómo obtener los datos de la base

En MySQL Workbench, conectado a cada servidor y con la base seleccionada, correr:

```sql
SELECT VERSION()                AS version_mysql,
       @@GLOBAL.sql_mode        AS sql_mode,
       @@character_set_database AS charset_base,
       @@collation_database     AS collation_base,
       @@lower_case_table_names AS lower_case_table_names,
       @@GLOBAL.time_zone       AS zona_horaria,
       @@system_time_zone       AS zona_horaria_sistema,
       DATABASE()               AS base;
```

Pegarle el resultado a Claude con "anótalo en ESPECIFICACIONES_AMBIENTES.md para QA" (o prod).
Son solo lecturas: no cambian nada en la base.

### Cómo exportar el esquema real (solo estructura, sin datos)

Sirve para que Claude pruebe los scripts contra las tablas reales y no contra una aproximación.
No lleva datos ni contraseñas, así que se puede guardar en el repo.

- **Workbench:** Server → Data Export → marcar `inventario_key_qa` → "Dump Structure Only" →
  "Export to Self-Contained File" → guardar como `esquema_real_qa.sql` en la raíz del proyecto.
- **Consola:**
  `mysqldump -h <host> -u <usuario> -p --no-data --skip-comments --routines --triggers inventario_key_qa > esquema_real_qa.sql`

Volver a exportarlo cuando se corra una migración que cambie tablas.

### Por qué la base real puede no coincidir con las entidades

Hibernate no administra el esquema (`ddl-auto: none` en dev, qa y docker): las tablas se crean y
cambian a mano con migraciones. Por eso una entidad puede decir una cosa y la tabla otra.

| Tabla | Diferencia conocida | Fuente |
|---|---|---|
| `producto` | `nombre`, `precio_costo`, `piezas`, `precio_venta` y `precio_rebaja` son NOT NULL en la base real; en la entidad no | comentario en `CargaImagenesServiceImpl` (alta del borrador) |
| varias | la entidad y la tabla se llaman distinto (`Imagen` → `imagenes_copy`, `Usuario` → `usuario_modificacion`…) | CLAUDE.md, "Trampa: nombres que no coinciden" |

### Convenciones de datos que un script tiene que respetar

- `habilitado` es `CHAR(1)`: `'1'` o `'0'`.
- Sin descuento, `precio_rebaja = precio_venta` (así lo guarda el alta de producto). El carrito
  solo ofrece "Otro precio" cuando `0 < precio_rebaja < precio_venta`.
- `variantes.precio_venta` / `precio_rebaja` en NULL = el artículo usa el precio del producto.
- Un código de barras que empieza con `BRD-` es un borrador de la carga rápida: no aparece en el
  listado de admin.
- Para aparecer en la tienda pública, un artículo necesita stock > 0, producto y artículo
  habilitados, `es_catalogo_interno = 0` y al menos una imagen en `variante_imagen`.
- Nunca `DELETE` de `producto` ni `variantes`: se da de baja con `habilitado = '0'`.

---

## 2. Backend (`proyecto_key`)

| Dato | Valor |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Dialecto Hibernate | `org.hibernate.dialect.MySQLDialect` |
| Esquema | `ddl-auto: none` (no lo toca Hibernate) |
| Tests | JUnit + H2 en memoria `MODE=MySQL`, esquema generado desde las entidades |
| Build | `mvn` (el `mvnw` del repo no tiene permiso de ejecución) |
| Servicios externos | Redis, RabbitMQ (no en `main`), `micro_imagenes` |

## 3. Front (`producto_venta_online`)

| Dato | Valor |
|---|---|
| Angular | 14.2 |
| TypeScript | 4.7 |
| Capacitor | 7 |
| Node (build Docker) | 20 |
| Tests unitarios | Karma + Jasmine |
| Playwright | no se instala en la raíz (`npm i` falla con ERESOLVE por Angular 14); va en su propia carpeta |
| API QA | `https://qa.backend.novedades-jade.com.mx/mis-productos` |
| API prod | `https://backend.novedades-jade.com.mx/mis-productos` |

## 4. Sesión de Claude en la nube

- **No tiene red hacia QA ni prod.** Claude no puede consultar las bases reales: cualquier dato
  de la base se lo pide al usuario con una consulta de solo lectura.
- Chromium para Playwright: `/opt/pw-browsers/chromium`.
- Docker está instalado pero sin daemon. Para probar SQL se instala MySQL 8 local (sección 5).

---

## 5. Checklist antes de entregar un script SQL

Existe porque el 2026-09-29 se entregó un script de datos de prueba sin revisar el esquema: usaba
columnas que no existen y **todos sus INSERT fallaban**.

1. Leer el `@Entity` / `@Column` de cada tabla que toca, y las diferencias conocidas de arriba.
2. Correrlo contra una base desechable con el esquema real (`esquema_real_qa.sql`). Si todavía no
   existe, usar el generado desde las entidades más las diferencias conocidas, y decirlo al entregar.
3. Usar la misma versión de MySQL que QA. Si la versión está pendiente, no usar funciones de
   ventana (`ROW_NUMBER`), CTE (`WITH`) ni nada que no exista en MySQL 5.7.
4. Correrlo dos veces: la segunda no debe insertar nada.
5. Si es solo para QA, cada INSERT lleva `AND DATABASE() = 'inventario_key_qa'`, y se prueba que
   en otra base inserta 0 filas.
6. Terminar con consultas de verificación que digan qué número se espera.
7. Empezar con `SET NAMES utf8mb4;`. Sin eso, si el cliente se conecta en latin1, "Única" se
   guarda como "Ãšnica" (pasó en la prueba local del 2026-09-29).
8. Workbench trae **Safe Updates** encendido: un `UPDATE`/`DELETE` con `JOIN` falla con error 1175.
   Guardar el valor, poner `SET SQL_SAFE_UPDATES = 0` y restaurarlo al final. Probar con
   `mysql --safe-updates` para simularlo.
9. No comparar columnas de texto de una tabla temporal contra columnas de tablas reales: si las
   colaciones no coinciden, MySQL falla con "Illegal mix of collations". Comparar contra literales,
   o usar tablas temporales solo con columnas numéricas.
10. Al entregarlo, decir cómo se probó y en qué base se corre. No decir "listo" ni "se puede
    correr dos veces" sin haberlo corrido.

### Comandos para la base desechable

```bash
# Esquema desde las entidades (solo si no hay esquema_real_qa.sql).
# El test termina en error porque H2 no entiende el DDL de MySQL; lo que importa es el archivo.
mvn -q -o test -Dtest=IProductosRepositoryBorradorTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=/tmp/schema_entidades.sql \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# MySQL 8 local
apt-get install -y mysql-server-8.0
mkdir -p /run/mysqld /tmp/mysql8data && chown -R mysql:mysql /run/mysqld /tmp/mysql8data
mysqld --initialize-insecure --user=mysql --datadir=/tmp/mysql8data
mysqld --no-defaults --user=mysql --datadir=/tmp/mysql8data --socket=/run/mysqld/mysqld.sock \
  --mysqlx=OFF --secure-file-priv= &

# Cargar el esquema generado. --force porque una FK hacia usuario_modificacion no carga en
# MySQL 8 (no afecta a las tablas del catálogo). Después aplicar a mano las diferencias conocidas
# (sección 1), p. ej. los NOT NULL de producto.
mysql -uroot -S /run/mysqld/mysqld.sock -e "create database inventario_key_qa"
mysql -uroot -S /run/mysqld/mysqld.sock --force inventario_key_qa < /tmp/schema_entidades.sql
```

Para probar que un script no toca datos reales: sembrar 2 o 3 productos "reales" con imágenes
antes de correrlo, sacar una huella (`MD5` de sus filas) antes y después, y compararlas. Crear
también una base con otro nombre (`inventario_key`) para probar que el script ahí no hace nada.
