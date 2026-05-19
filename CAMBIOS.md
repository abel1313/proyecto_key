# Cambios — Rediseño de Ventas, Pedidos y Detalles
for /f "tokens=5" %a in ('netstat -ano ^| findstr :9091') do taskkill /PID %a /F

## Archivos modificados

### entity/DetallePedido.java
- variante_id ahora es nullable=false — siempre se debe saber que variante se pidio
- cantidad ahora tiene @Column(nullable=false) — evita que quede null en BD

### entity/Pedido.java
- cliente_id ya no tiene nullable=false — puede ser null cuando el comprador no esta registrado
- Se agrego campo clienteSinRegistro con @JoinColumn(name="cliente_sin_registro_id")

### entity/Venta.java
- List<DetalleVenta> reemplazada por List<DetalleVentaVariante>

### service/VentaServiceImpl.java
- Se elimino IDetalleVentaRepository del constructor (nunca se uso)
- Se corrigio el bug donde pedido.setCliente(null) rompia la BD en ventas sin cliente registrado
- saveVentaDetalle ahora siempre crea un Pedido (los 3 escenarios)
- Se genera DetallePedido (que se pidio) y DetalleVentaVariante (datos financieros) en el mismo flujo

## Archivos nuevos

### entity/DetalleVentaVariante.java
- Entidad que mapea la tabla detalle_venta_variantes que ya existia en BD sin clase Java
- Campos: venta_id, variante_id, cantidad, precioUnitario, subTotal, precioCosto, ganancia, fechaVenta

### repository/IDetalleVentaVarianteRepository.java
- Repositorio para DetalleVentaVariante

## Archivos deprecados (no se usan mas, no se eliminan aun)

### entity/DetalleVenta.java
- Ya no se usa en el flujo de ventas directas
- La tabla detalle_venta queda sin nuevos registros

### repository/IDetalleVentaRepository.java
- Sin uso activo

---

## Flujo resultante

ESCENARIO 1 — Pedido desde app (cliente registrado)
  App crea Pedido (PENDIENTE) + DetallePedido[]
  Cliente paga en local: Venta + DetalleVentaVariante[] + Pedido pasa a ENTREGADO

ESCENARIO 2 — Venta directa, cliente registrado
  Cajero: Pedido (ENTREGADO) + DetallePedido[] + Venta + DetalleVentaVariante[]

ESCENARIO 3 — Venta directa, cliente sin registro
  Cajero captura datos: ClienteSinRegistro (para rifas)
  Pedido (cliente=null, clienteSinRegistro=X) + DetallePedido[]
  Venta (cliente=null, clienteSinRegistro=X) + DetalleVentaVariante[]

## Reporte mensual unificado

Con todos los escenarios creando Pedido, el reporte es una sola query:

  SELECT p.*, c.nombre_persona as cliente, csr.nombre_persona as visitante
  FROM pedidos p
  LEFT JOIN clientes c ON c.id = p.cliente_id
  LEFT JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
  WHERE p.fecha_pedido BETWEEN '2026-05-01' AND '2026-05-31'
  ORDER BY p.fecha_pedido DESC;