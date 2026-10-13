package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.escenarios;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.DatosDelModelo;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.EscenariosPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioArticuloCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.models.pedidos.ClienteDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.DetallePedidosDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R8, R16 y R17: los pedidos de una prueba salen de los mismos servicios que la pantalla
 * ({@code savePedido} del carrito, {@code saveVentaDetalle} de la venta directa y
 * {@code deletePedidoById} del boton ✖ Cancelar). No se escribe ninguna tabla de pedidos a mano.
 *
 * <p>R18: el catálogo de una prueba se da de alta con el generador (CatalogoPruebaPort, JDBC como
 * el resto del catálogo de prueba) y aquí solo se deja en el estado que pide la prueba: stock del
 * modelo, modelo deshabilitado, artículo viejo deshabilitado con stock. El precio con descuento pasa
 * por el mismo caso de uso que el 💲 de la tarjeta. Solo sobre modelos con marca "Prueba QA".
 */
@Component
@RequiredArgsConstructor
public class EscenariosPruebaAdapter implements EscenariosPruebaPort {

    static final String ZONA = "Zona Prueba QA";
    static final String NOMBRE_CLIENTE = "Cliente Prueba QA";
    /** .invalid es un dominio reservado: ningun correo de confirmacion le llega a nadie. */
    static final String CORREO_CLIENTE = "cliente.prueba.qa@ejemplo.invalid";

    private final ILugarEntregaRepository lugares;
    private final IClienteRepository clientes;
    private final IPedidoService pedidoService;
    private final VentaServiceImpl ventas;
    private final CambiarPrecioArticuloCasoUso precios;
    private final AvisarCambioCatalogoPort catalogoCambio;
    private final JdbcTemplate jdbc;

    @Override
    public int zonaDePrueba() {
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM lugares_entrega WHERE nombre = ? ORDER BY id LIMIT 1", Integer.class, ZONA);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        LugarEntrega zona = new LugarEntrega();
        zona.setNombre(ZONA);
        zona.setEsRecogerEnTienda(false);
        zona.setCostoEnvio(0.0);
        return lugares.save(zona).getId();
    }

    @Override
    public int clienteConCuentaDePrueba() {
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM clientes WHERE correo_electronico = ? ORDER BY id LIMIT 1", Integer.class, CORREO_CLIENTE);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        Cliente c = new Cliente();
        c.setNombrePersona(NOMBRE_CLIENTE);
        c.setApeidoPaterno("Prueba");
        c.setNumeroTelefonico("0000000000");
        c.setCorreoElectronico(CORREO_CLIENTE);
        c.setCorreoVerificado(true);
        c.setRecibirCorreos(false);
        c.setRecibirPromociones(false);
        return clientes.save(c).getId();
    }

    @Override
    public int pedidoDelCliente(int clienteId, List<LineaDePedido> lineas, Integer lugarEntregaId) {
        PedidosDTOPedido req = new PedidosDTOPedido();
        ClienteDTOPedido cliente = new ClienteDTOPedido();
        cliente.setId(clienteId);
        req.setCliente(cliente);
        req.setFechaPedido(LocalDate.now());
        req.setEstadoPedido("Pendiente");
        req.setTipoPedido("NORMAL");
        req.setObservaciones(PedidosPruebaPort.MARCA_PEDIDO);
        req.setLugarEntregaId(lugarEntregaId);
        req.setDetalles(lineas.stream().map(l -> {
            DetallePedidosDTOPedido d = new DetallePedidosDTOPedido();
            d.setVarianteId(l.articuloId());
            d.setCantidad(l.cantidad());
            d.setPrecioUnitario(l.precio());
            d.setSubTotal(l.subtotal());
            return d;
        }).toList());
        try {
            Pedido pedido = pedidoService.savePedido(req, null);
            return pedido.getId();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public int ventaDirecta(String tipoPedido, int clienteSinRegistroId, int usuarioId, List<LineaDePedido> lineas,
                            Integer lugarEntregaId, boolean seLoLlevo) {
        VentaDirectaRequest req = new VentaDirectaRequest();
        req.setUsuarioId(usuarioId);
        req.setClienteSinRegistroId(clienteSinRegistroId);
        req.setTipoPedido(tipoPedido);
        req.setObservaciones(PedidosPruebaPort.MARCA_PEDIDO);
        req.setLugarEntregaId(lugarEntregaId);
        req.setEntregado(seLoLlevo);
        boolean contado = "NORMAL".equals(tipoPedido);
        if (contado) {
            req.setPagosYMesesId(efectivo());
        }
        req.setDetalles(lineas.stream().map(l -> {
            DetalleVentaDto d = new DetalleVentaDto();
            d.setVarianteId(l.articuloId());
            d.setCantidad(l.cantidad());
            d.setPrecioVenta(l.precio());
            d.setSubTotal(l.subtotal());
            return d;
        }).toList());
        try {
            VentaDirectaResponse resp = ventas.saveVentaDetalle(req);
            // De contado la venta directa regresa la venta, no el pedido: el pedido es el de esa venta.
            Integer pedidoId = contado && resp.getVentaId() != null
                    ? jdbc.queryForObject("SELECT pedido_id FROM ventas WHERE id = ?", Integer.class, resp.getVentaId())
                    : resp.getPedidoId();
            if (pedidoId == null) {
                throw new IllegalStateException("La venta directa no regresó el número de pedido");
            }
            return pedidoId;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** La forma de pago "Efectivo", la misma que busca el generador de datos (PedidosPruebaVentaAdapter). */
    private int efectivo() {
        List<Integer> ids = jdbc.queryForList(
                "SELECT pm.id FROM pagos_y_meses pm JOIN tipo_pago tp ON tp.id = pm.tipo_pago_id "
                        + "WHERE LOWER(TRIM(tp.forma_pago)) = 'efectivo' ORDER BY pm.id LIMIT 1", Integer.class);
        if (ids.isEmpty()) {
            throw new IllegalStateException("No hay forma de pago 'Efectivo' en pagos_y_meses: no se puede crear una venta de contado");
        }
        return ids.get(0);
    }

    @Override
    public void cancelar(int pedidoId) {
        pedidoService.deletePedidoById(pedidoId, "CLIENTE_AVISO");
    }

    // ── Catálogo (R18) ─────────────────────────────────────────────────────────

    @Override
    public DatosDelModelo datosDelModelo(int articuloId) {
        return jdbc.queryForObject("SELECT p.id, p.nombre, cb.codigo_barras FROM variantes v "
                        + "JOIN producto p ON p.id = v.producto_id "
                        + "LEFT JOIN codigo_barras cb ON cb.id = p.codigo_barras_id WHERE v.id = ?",
                (rs, n) -> new DatosDelModelo(rs.getInt("id"), rs.getString("nombre"), rs.getString("codigo_barras")),
                articuloId);
    }

    @Override
    public void stockDelModelo(int articuloId, int stock) {
        int modelo = modeloDePrueba(articuloId);
        jdbc.update("UPDATE producto SET stock = ?, piezas = ? WHERE id = ?", stock, stock, modelo);
    }

    @Override
    public void habilitarModelo(int articuloId, boolean habilitado) {
        int modelo = modeloDePrueba(articuloId);
        jdbc.update("UPDATE producto SET habilitado = ? WHERE id = ?", habilitado ? "1" : "0", modelo);
    }

    @Override
    public void articuloDeshabilitadoConStock(int articuloId, int stock) {
        modeloDePrueba(articuloId);
        jdbc.update("UPDATE variantes SET habilitado = '0', stock = ? WHERE id = ?", stock, articuloId);
    }

    @Override
    public void precioDelArticulo(int articuloId, double precioVenta, double precioRebaja, boolean usarDescuento) {
        modeloDePrueba(articuloId);
        precios.cambiar(articuloId, precioVenta, precioRebaja, usarDescuento);
    }

    @Override
    public void limpiarCache() {
        catalogoCambio.catalogoCambio();
    }

    /** El modelo del artículo, solo si es de prueba (marca "Prueba QA"): nunca se toca uno real. */
    private int modeloDePrueba(int articuloId) {
        List<Integer> ids = jdbc.queryForList("SELECT p.id FROM variantes v JOIN producto p ON p.id = v.producto_id "
                + "WHERE v.id = ? AND p.marca = ?", Integer.class, articuloId, ModeloDePrueba.MARCA);
        if (ids.isEmpty()) {
            throw new IllegalStateException("El artículo " + articuloId + " no es de prueba: no se modifica");
        }
        return ids.get(0);
    }
}
