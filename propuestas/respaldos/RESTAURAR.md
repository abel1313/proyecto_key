# Probar la restauración (lo más importante de un respaldo)
Un respaldo que nunca se restauró no es un respaldo. Una vez al mes, en una base **de prueba**:
1. Bajar la copia más reciente y descifrarla.
2. `gunzip -c inventario_key_AAAA-MM-DD.sql.gz | mysql -h <host-de-prueba> -u <usuario> -p inventario_key_restaurada`
3. Comprobar: número de pedidos, último pedido, suma de `total_pedido` de la semana y que el login funcione contra esa base.
4. Anotar cuánto tardó (meta sugerida: menos de 4 h) y cuánto dinero/pedidos faltaban (meta: menos de 24 h de datos).
