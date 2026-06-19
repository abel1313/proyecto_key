# Plan: Módulo Ventas (búsqueda) + Módulo Gastos

> Acceso: solo administrador en ambos módulos.

---

## 1. Ventas — búsqueda y consulta

### Qué se va a hacer
Nuevo endpoint de búsqueda paginada de ventas con filtro por fecha.  
Las ventas aquí son **solo ventas concretadas** (estado = "Entregado").  
Los pedidos pendientes NO se incluyen — eso es otro módulo distinto.

### Comportamiento
- Sin parámetro de fecha → devuelve las ventas del **día actual**
- Con parámetro de fecha (o rango) → filtra por ese periodo
- Resultado paginado
- Cada venta incluye el detalle completo de los productos vendidos

### Campos del response

**Cabecera de la venta:**
| Campo | Fuente en BD |
|-------|-------------|
| `ventaId` | `ventas.id` |
| `fechaVenta` | `ventas.fecha_venta` |
| `totalVenta` | `ventas.total_venta` |
| `gananciaTotal` | `ventas.ganancia_total` |
| `estadoVenta` | `ventas.estado_venta` |
| `formaPago` | `ventas → pagos_y_meses → tipo_pago.forma_pago` |
| `clienteNombre` | `clientes.nombre_persona + apellidos` (o `clientes_sin_registro` si no está registrado) |
| `vendedor` | `ventas → usuario_modificacion.username` |

**Detalle por producto (lista dentro de cada venta):**
| Campo | Fuente en BD |
|-------|-------------|
| `productoNombre` | `detalle_venta_variantes → variantes → producto.nombre` |
| `talla` | `variantes.talla` |
| `color` | `variantes.color` |
| `cantidad` | `detalle_venta_variantes.cantidad` |
| `precioUnitario` | `detalle_venta_variantes.precio_unitario` |
| `precioCosto` | `detalle_venta_variantes.precio_costo` |
| `subTotal` | `detalle_venta_variantes.sub_total` |
| `ganancia` | `detalle_venta_variantes.ganancia` |

### Endpoint planeado
```
GET /v1/ventas/buscar?fecha=2026-06-18&page=0&size=10
GET /v1/ventas/buscar?fechaInicio=2026-06-01&fechaFin=2026-06-18&page=0&size=10
```

---

## 2. Gastos — módulo nuevo (CRUD)

### Qué se va a hacer
Entidad nueva `Gasto` para registrar cualquier salida de dinero del negocio  
(inventario, renta, luz, transporte, etc.) y poder ver la rentabilidad real del negocio.

### Entidad `Gasto`
| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `id` | Integer | sí (PK) | Auto generado |
| `descripcion` | String | sí | Qué fue el gasto (ej: "Surtido proveedor Martínez") |
| `monto` | Double | sí | Cuánto se pagó |
| `fecha` | LocalDate | sí | Cuándo ocurrió el gasto |
| `categoria` | String (enum) | sí | Ver categorías abajo |
| `proveedor` | String | no | Nombre del proveedor si aplica |
| `comprobante` | String | no | Número de factura o recibo |
| `notas` | String | no | Cualquier detalle extra |

**Categorías:**
- `INVENTARIO` — compra de mercancía/surtido
- `OPERATIVO` — renta, luz, agua, etc.
- `SERVICIOS` — empaque, transporte, comisiones
- `OTROS` — cualquier gasto que no encaje

### Endpoints planeados
```
POST   /v1/gastos/save                    → crear gasto
GET    /v1/gastos/buscar?fecha=...        → listar paginado con filtro fecha/categoria
PUT    /v1/gastos/update/{id}             → editar gasto
DELETE /v1/gastos/delete/{id}            → eliminar gasto
GET    /v1/gastos/reporte?fechaInicio=...&fechaFin=...  → resumen del periodo
```

---

## 3. Reporte de rentabilidad (ventas + gastos juntos)

### Concepto clave
La `gananciaTotal` de cada venta **ya descuenta el precio de costo del producto** en el momento de la venta. Eso es la ganancia exacta por producto.

Los gastos del módulo de gastos son el **flujo de efectivo del negocio** (renta, surtido, etc.) y se registran cuando ocurren — no necesariamente en el mismo mes en que se vende ese inventario.

Por eso el reporte los muestra **separados**, no mezclados:

```
Reporte periodo: 01-jun al 18-jun
─────────────────────────────────────────────
Ventas brutas (ingresos):         $15,400
Ganancia por producto:             $4,200   ← exacta, descuenta costo de cada pieza vendida
─────────────────────────────────────────────
Gastos del periodo:
  INVENTARIO  (surtido):          $5,000
  OPERATIVO   (renta + luz):      $2,400
  SERVICIOS   (empaque):            $300
  Total gastos:                   $7,700
─────────────────────────────────────────────
Ganancia estimada neta:           -$3,500   ← puede ser negativo si surtiste ese mes
                                               pero aún quedan piezas por vender
```

> **Nota:** la ganancia neta puede verse negativa en meses de surtido fuerte — eso es normal.
> La ganancia **por producto** es la cifra confiable porque considera el costo real de cada pieza vendida.

### Por qué no se "juntan" los gastos con las ventas en tiempo real
El inventario rota a destiempo: surtiste en enero, vendes en febrero y marzo.
Si juntas el gasto de enero con las ventas de enero parece que perdiste dinero,
pero en realidad el costo ya está capturado en `precioCosto` de cada venta futura.
El reporte por separado evita esa confusión.

---

## 4. Estado de implementación

| Módulo | Estado |
|--------|--------|
| Endpoint búsqueda ventas | ⏳ Pendiente |
| Entidad + repositorio Gasto | ⏳ Pendiente |
| CRUD gastos | ⏳ Pendiente |
| Endpoint reporte rentabilidad | ⏳ Pendiente |
| Script migración BD (tabla gastos) | ⏳ Pendiente |
