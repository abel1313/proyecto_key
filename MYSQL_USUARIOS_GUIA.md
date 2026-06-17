# Guía MySQL — Usuarios, Permisos y Bases de Datos

> Documento de referencia para saber cómo crear, modificar y eliminar
> usuarios de MySQL y controlar qué base de datos puede usar cada uno.

---

## ¿Qué es un usuario de MySQL?

Un usuario de MySQL tiene dos partes: **nombre** y **desde dónde conecta**.

```
'user_ventas'@'%'         → user_ventas desde cualquier IP
'user_ventas'@'localhost' → user_ventas solo desde la misma máquina
'user_ventas'@'10.42.0.1' → user_ventas solo desde esa IP específica
```

El `%` significa "cualquier IP" — se usa cuando la app corre en K8s porque
los pods tienen IPs que cambian cada vez que se reinician.

---

## Ver qué usuarios existen

```bash
mysql -u root -p -e "SELECT user, host FROM mysql.user;"
```

## Ver a qué bases tiene acceso un usuario

```bash
mysql -u root -p -e "SHOW GRANTS FOR 'user_ventas'@'%';"
```

---

## Crear un usuario nuevo

```bash
mysql -u root -p -e "CREATE USER 'nombre_usuario'@'%' IDENTIFIED BY 'contraseña';"
```

**Ejemplo:**
```bash
mysql -u root -p -e "CREATE USER 'user_ventas'@'%' IDENTIFIED BY 'MiContraseña123';"
```

---

## Dar permisos a un usuario sobre una base de datos

### Permisos completos (para apps en producción)
```bash
mysql -u root -p -e "GRANT ALL PRIVILEGES ON nombre_base.* TO 'nombre_usuario'@'%';"
```

### Solo lectura (para reportes o acceso de consulta)
```bash
mysql -u root -p -e "GRANT SELECT ON nombre_base.* TO 'nombre_usuario'@'%';"
```

### Lectura y escritura (sin poder crear/borrar tablas)
```bash
mysql -u root -p -e "GRANT SELECT, INSERT, UPDATE, DELETE ON nombre_base.* TO 'nombre_usuario'@'%';"
```

### Aplicar los cambios (siempre ejecutar después de dar permisos)
```bash
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

---

## Cambiar un usuario de una base a otra

No se "mueve" — se quitan los permisos de la base vieja y se dan en la nueva.

```bash
# 1. Quitar permisos de la base vieja
mysql -u root -p -e "REVOKE ALL PRIVILEGES ON base_vieja.* FROM 'nombre_usuario'@'%';"

# 2. Dar permisos en la base nueva
mysql -u root -p -e "GRANT ALL PRIVILEGES ON base_nueva.* TO 'nombre_usuario'@'%';"

# 3. Aplicar
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

**Ejemplo real — mover user_ventas_qa de inventario_key_qa a inventario_key_nueva:**
```bash
mysql -u root -p -e "REVOKE ALL PRIVILEGES ON inventario_key_qa.* FROM 'user_ventas_qa'@'%';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON inventario_key_nueva.* TO 'user_ventas_qa'@'%';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

---

## Dar acceso a una base adicional (sin quitar la anterior)

Si el usuario ya tiene acceso a una base y quieres que también acceda a otra:

```bash
mysql -u root -p -e "GRANT ALL PRIVILEGES ON otra_base.* TO 'nombre_usuario'@'%';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

---

## Cambiar la contraseña de un usuario

```bash
mysql -u root -p -e "ALTER USER 'nombre_usuario'@'%' IDENTIFIED BY 'nueva_contraseña';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

---

## Quitar permisos de una base específica

```bash
mysql -u root -p -e "REVOKE ALL PRIVILEGES ON nombre_base.* FROM 'nombre_usuario'@'%';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

---

## Eliminar un usuario completamente

```bash
mysql -u root -p -e "DROP USER 'nombre_usuario'@'%';"
```

---

## Ver todos los permisos de todos los usuarios de una vez

```bash
mysql -u root -p -e "
SELECT
  GRANTEE,
  TABLE_SCHEMA as base_de_datos,
  GROUP_CONCAT(PRIVILEGE_TYPE) as permisos
FROM information_schema.SCHEMA_PRIVILEGES
GROUP BY GRANTEE, TABLE_SCHEMA
ORDER BY TABLE_SCHEMA;
"
```

---

## Resumen rápido de comandos

| Qué quiero hacer | Comando |
|---|---|
| Ver usuarios | `SELECT user, host FROM mysql.user;` |
| Ver permisos de un usuario | `SHOW GRANTS FOR 'usuario'@'%';` |
| Crear usuario | `CREATE USER 'usuario'@'%' IDENTIFIED BY 'pass';` |
| Dar permisos completos | `GRANT ALL PRIVILEGES ON base.* TO 'usuario'@'%';` |
| Dar solo lectura | `GRANT SELECT ON base.* TO 'usuario'@'%';` |
| Quitar permisos de una base | `REVOKE ALL PRIVILEGES ON base.* FROM 'usuario'@'%';` |
| Cambiar contraseña | `ALTER USER 'usuario'@'%' IDENTIFIED BY 'nueva_pass';` |
| Eliminar usuario | `DROP USER 'usuario'@'%';` |
| Aplicar cambios | `FLUSH PRIVILEGES;` |
