package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.escenarios;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.DatosDelModelo;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.NombresDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.EscenariosPruebaPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioArticuloCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.models.pedidos.ClienteDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.DetallePedidosDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
 *
 * <p>R20: cada pedido lleva un cliente con el nombre exacto de la prueba y la clave ("Prueba 24 K4").
 * R21: para reiniciar una prueba se buscan sus pedidos por los artículos que llevan (modelos
 * "QA Prueba {prueba} …" de marca "Prueba QA"), se cancelan con el servicio real y sus modelos se dan de
 * baja; nunca se borra una fila.
 */
@Component
@RequiredArgsConstructor
public class EscenariosPruebaAdapter implements EscenariosPruebaPort {

    static final String ZONA = "Zona Prueba QA";

    private final ILugarEntregaRepository lugares;
    private final IClienteRepository clientes;
    private final IClienteSinRegistroRepository clientesSinRegistro;
    private final IPedidoService pedidoService;
    private final VentaServiceImpl ventas;
    private final CambiarPrecioArticuloCasoUso precios;
    private final AvisarCambioCatalogoPort catalogoCambio;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;

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

    /**
     * Un cliente con cuenta, correo verificado y datos completos (lo que exige el carrito), llamado
     * "Prueba 16 A". El correo termina en .invalid (dominio reservado: no le llega a nadie) y no recibe correos.
     */
    private int clienteConCuenta(String prueba, String clave) {
        String correo = NombresDePrueba.correo(prueba, clave);
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM clientes WHERE correo_electronico = ? ORDER BY id LIMIT 1", Integer.class, correo);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        Cliente c = new Cliente();
        c.setNombrePersona(NombresDePrueba.cliente(prueba, clave));
        c.setApeidoPaterno("QA");
        c.setNumeroTelefonico("0000000000");
        c.setCorreoElectronico(correo);
        c.setCorreoVerificado(true);
        c.setRecibirCorreos(false);
        c.setRecibirPromociones(false);
        return clientes.save(c).getId();
    }

    /** Un cliente de venta directa llamado "Prueba 24 K4", sin correo ni teléfono (R10). Lo reusa si ya existe. */
    private int clienteSinRegistro(String prueba, String clave) {
        String nombre = NombresDePrueba.cliente(prueba, clave);
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM clientes_sin_registro WHERE nombre_persona = ? ORDER BY id LIMIT 1", Integer.class, nombre);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        ClienteSinRegistro c = new ClienteSinRegistro();
        c.setNombrePersona(nombre);
        c.setApeidoPaterno("QA");
        return clientesSinRegistro.save(c).getId();
    }

    @Override
    public int pedidoDelCliente(String prueba, String clave, List<LineaDePedido> lineas, Integer lugarEntregaId) {
        PedidosDTOPedido req = new PedidosDTOPedido();
        ClienteDTOPedido cliente = new ClienteDTOPedido();
        cliente.setId(clienteConCuenta(prueba, clave));
        req.setCliente(cliente);
        req.setFechaPedido(LocalDate.now());
        req.setEstadoPedido("Pendiente");
        req.setTipoPedido("NORMAL");
        req.setObservaciones(NombresDePrueba.observacion(prueba, clave));
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
    public int ventaDirecta(String tipoPedido, String prueba, String clave, int usuarioId, List<LineaDePedido> lineas,
                            Integer lugarEntregaId, boolean seLoLlevo) {
        VentaDirectaRequest req = ventaBase(tipoPedido, prueba, clave, usuarioId, lineas, lugarEntregaId, seLoLlevo);
        req.setClienteSinRegistroId(clienteSinRegistro(prueba, clave));
        return guardarVenta(req, "NORMAL".equals(tipoPedido));
    }

    /**
     * Igual que la Venta directa de la pantalla sin registrar al cliente: manda {@code clienteId} con el perfil
     * de cliente del administrador (la pantalla lo busca siempre con {@code buscarClientePorIdUsuario}) y el
     * nombre escrito en {@code nombreReceptor}. El pedido queda con la cuenta del admin y ese nombre.
     */
    @Override
    public int ventaDirectaConNombreEscrito(String tipoPedido, String prueba, String clave, int usuarioId,
                                            List<LineaDePedido> lineas, Integer lugarEntregaId, boolean seLoLlevo) {
        VentaDirectaRequest req = ventaBase(tipoPedido, prueba, clave, usuarioId, lineas, lugarEntregaId, seLoLlevo);
        req.setClienteId(perfilDeClienteDelAdmin(usuarioId));
        req.setNombreReceptor(NombresDePrueba.cliente(prueba, clave));
        return guardarVenta(req, "NORMAL".equals(tipoPedido));
    }

    private int perfilDeClienteDelAdmin(int usuarioId) {
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM clientes WHERE usuario_id = ? ORDER BY id LIMIT 1", Integer.class, usuarioId);
        if (ids.isEmpty()) {
            throw new IllegalStateException("El administrador no tiene un perfil de cliente (Mis datos): sin él la "
                    + "Venta directa tampoco deja vender sin registrar al cliente. Completa tus datos y vuelve a intentar.");
        }
        return ids.get(0);
    }

    private VentaDirectaRequest ventaBase(String tipoPedido, String prueba, String clave, int usuarioId,
                                          List<LineaDePedido> lineas, Integer lugarEntregaId, boolean seLoLlevo) {
        VentaDirectaRequest req = new VentaDirectaRequest();
        req.setUsuarioId(usuarioId);
        req.setTipoPedido(tipoPedido);
        req.setObservaciones(NombresDePrueba.observacion(prueba, clave));
        req.setLugarEntregaId(lugarEntregaId);
        req.setEntregado(seLoLlevo);
        if ("NORMAL".equals(tipoPedido)) {
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
        return req;
    }

    /** Guarda la venta con el servicio real y regresa el número del pedido que quedó. */
    private int guardarVenta(VentaDirectaRequest req, boolean contado) {
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

    // ── Reiniciar una prueba (R21) ─────────────────────────────────────────────

    @Override
    public List<Integer> pedidosDeLaPrueba(String prueba) {
        return jdbc.queryForList("""
                SELECT DISTINCT p.id FROM pedidos p
                  JOIN detalle_pedidos d ON d.pedido_id = p.id
                  JOIN variantes v ON v.id = d.variante_id
                  JOIN producto m ON m.id = v.producto_id
                WHERE m.marca = ? AND m.nombre LIKE ? AND COALESCE(p.estado_pedido, '') <> 'cancelado'
                ORDER BY p.id""", Integer.class, ModeloDePrueba.MARCA, NombresDePrueba.prefijoModelos(prueba) + "%");
    }

    @Override
    public int darDeBajaCatalogoDeLaPrueba(String prueba) {
        // Igual que la baja del generador (R14): stock en 0, sin ligas de imagen, nunca se borra una fila ni una imagen.
        String modelos = "SELECT m.id FROM producto m WHERE m.marca = ? AND m.nombre LIKE ?";
        Object[] args = {ModeloDePrueba.MARCA, NombresDePrueba.prefijoModelos(prueba) + "%"};
        Integer dados = transaccion.execute(estado -> {
            jdbc.update("DELETE FROM variante_imagen WHERE variante_id IN "
                    + "(SELECT v.id FROM variantes v WHERE v.producto_id IN (" + modelos + "))", args);
            jdbc.update("DELETE FROM producto_imagen_copy WHERE producto_id IN (" + modelos + ")", args);
            jdbc.update("UPDATE variantes SET habilitado = '0', stock = 0 WHERE habilitado = '1' "
                    + "AND producto_id IN (" + modelos + ")", args);
            // MySQL no deja leer en un subquery la misma tabla que se actualiza (error 1093): la tabla
            // derivada "x" se materializa antes del UPDATE y lo evita.
            return jdbc.update("UPDATE producto SET habilitado = '0', stock = 0 WHERE habilitado = '1' AND id IN ("
                    + "SELECT x.id FROM (" + modelos + ") x)", args);
        });
        return dados == null ? 0 : dados;
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
