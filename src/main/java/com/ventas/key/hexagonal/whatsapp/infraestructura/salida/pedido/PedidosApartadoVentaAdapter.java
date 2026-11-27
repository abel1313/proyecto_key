package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.articulo.dominio.excepcion.ArticuloNoVendibleException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoDeCreacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PedidosApartadoPort;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Crea el Apartado con el MISMO servicio de «Venta directa» ({@code VentaServiceImpl.saveVentaDetalle}) y un cliente
 * sin registro; nunca con un INSERT. Así el stock, el precio de catálogo, las reglas del Apartado, el caché y la
 * versión de las listas de pedidos los pone el código de siempre. No hay ningún endpoint nuevo: nadie de afuera puede
 * llegar aquí (regla R18).
 *
 * <p>Todo (cliente + pedido) va en UNA transacción: si falla la venta no queda un cliente suelto.
 * El «vendedor» que pide el servicio es la cuenta {@code whatsapp.bot.usuario-id}; si no está puesta, la primera cuenta
 * ROLE_ADMIN activa. Solo sirve para validar que exista: un Apartado no genera venta.</p>
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class PedidosApartadoVentaAdapter implements PedidosApartadoPort {

    private static final Logger log = LoggerFactory.getLogger(PedidosApartadoVentaAdapter.class);

    private final VentaServiceImpl ventas;
    private final IClienteSinRegistroRepository clientes;
    private final IUsuarioRepository usuarios;
    private final TransactionTemplate transaccion;
    private final int usuarioConfigurado;
    private final JdbcTemplate jdbc;

    /** Con lo que empiezan las observaciones de todo pedido del bot: de aquí se reconoce de qué número vino. */
    public static final String PREFIJO_OBSERVACIONES = "Pedido hecho por WhatsApp +";

    public PedidosApartadoVentaAdapter(VentaServiceImpl ventas, IClienteSinRegistroRepository clientes,
                                       IUsuarioRepository usuarios, PlatformTransactionManager tm,
                                       @Value("${whatsapp.bot.usuario-id:0}") int usuarioConfigurado, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.ventas = ventas;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.transaccion = new TransactionTemplate(tm);
        this.usuarioConfigurado = usuarioConfigurado;
    }

    @Override
    public ResultadoDeCreacion crearApartado(BorradorDePedido b) {
        Optional<Integer> vendedor = vendedor();
        if (vendedor.isEmpty()) {
            return new ResultadoDeCreacion.Fallo("No hay una cuenta para registrar el pedido: pon whatsapp.bot.usuario-id "
                    + "o crea una cuenta ROLE_ADMIN activa");
        }
        try {
            VentaDirectaResponse resp = transaccion.execute(estado -> {
                ClienteSinRegistro cliente = clienteDelPedido(b);
                VentaDirectaRequest req = new VentaDirectaRequest();
                req.setUsuarioId(vendedor.get());
                req.setClienteSinRegistroId(cliente.getId());
                req.setTipoPedido("APARTADO");   // siempre: el bot nunca anota dinero (regla R16)
                req.setObservaciones(b.observaciones());
                req.setNombreReceptor(b.recibe());
                req.setDireccionEntrega(b.direccion());
                req.setLugarEntregaId(b.lugar() == null ? null : b.lugar().id());
                req.setUrlFacebook(b.urlWhatsapp());   // el campo «link» del pedido: su WhatsApp directo
                req.setDetalles(detalles(b.lineas()));
                return ventas.saveVentaDetalle(req);
            });
            if (resp == null || resp.getPedidoId() == null) {
                return new ResultadoDeCreacion.Fallo("El servicio de ventas no devolvió el número de pedido");
            }
            // Lo REAL: las líneas y el total que cobró el servicio (pudo bajar por campaña o precio de grupo).
            List<ResultadoDeCreacion.LineaCobrada> reales = resp.getRenglones() == null ? List.of()
                    : resp.getRenglones().stream().map(r -> new ResultadoDeCreacion.LineaCobrada(
                            r.getNombre() == null ? "Artículo" : r.getNombre(), r.getCantidad() == null ? 0 : r.getCantidad(),
                            r.getPrecioNormal() == null ? 0 : r.getPrecioNormal(),
                            r.getPrecioCobrado() == null ? 0 : r.getPrecioCobrado())).toList();
            return new ResultadoDeCreacion.Creado(resp.getPedidoId(), resp.getTotalVenta() == null ? b.total() : resp.getTotalVenta(), reales);
        } catch (ArticuloNoVendibleException e) {
            return new ResultadoDeCreacion.NoDisponible(e.getMessage());
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo crear el Apartado ({})", e.toString());
            return new ResultadoDeCreacion.Fallo(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Reutiliza al cliente de un pedido anterior (mismo teléfono, correo y nombre) o crea uno nuevo. */
    private ClienteSinRegistro clienteDelPedido(BorradorDePedido b) {
        Optional<ClienteSinRegistro> previo = clientes.findFirstByNumeroTelefonicoOrderByIdDesc(b.telefono());
        if (previo.isPresent() && mismo(previo.get().getCorreoElectronico(), b.correo())
                && mismo(previo.get().getNombrePersona(), b.nombre())) {
            return previo.get();
        }
        ClienteSinRegistro c = new ClienteSinRegistro();
        c.setNombrePersona(b.nombre());
        c.setNumeroTelefonico(b.telefono());
        c.setCorreoElectronico(b.correo());
        c.setCorreoVerificado(false);   // el correo se confirmó en la conversación, pero no con un código
        return clientes.save(c);
    }

    private static boolean mismo(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static List<DetalleVentaDto> detalles(List<LineaDePedido> lineas) {
        return lineas.stream().map(l -> {
            DetalleVentaDto d = new DetalleVentaDto();
            d.setVarianteId(l.articuloId());
            d.setCantidad(l.cantidad());
            d.setPrecioVenta(l.precioUnitario());
            d.setSubTotal(l.subtotal());
            return d;
        }).toList();
    }

    @Override
    public int contarAbiertosDesde(String numeroDeWhatsapp) {
        if (numeroDeWhatsapp == null || !numeroDeWhatsapp.matches("\\d{6,20}")) {
            return 0;
        }
        String base = PREFIJO_OBSERVACIONES + numeroDeWhatsapp;
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pedidos
                WHERE (observaciones = ? OR observaciones LIKE ?)
                  AND tipo_pedido IN ('APARTADO', 'FIADO')
                  AND (estado_pedido IS NULL OR UPPER(estado_pedido) <> 'CANCELADO')
                  AND (entregado IS NULL OR entregado = FALSE)
                """, Integer.class, base, base + " |%");
        return n == null ? 0 : n;
    }

    private Optional<Integer> vendedor() {
        if (usuarioConfigurado > 0) {
            return usuarios.findById(usuarioConfigurado).map(Usuario::getId);
        }
        // Determinista: la cuenta ROLE_ADMIN activa con el id más bajo.
        return usuarios.findByRoles_NombreRolAndEnabledTrue("ROLE_ADMIN").stream()
                .map(Usuario::getId).filter(java.util.Objects::nonNull).sorted().findFirst();
    }
}
