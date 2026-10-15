package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.pedidos;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.TipoPedidoPrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.models.abonos.AbonoRequest;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R8: los pedidos de prueba se crean con la misma venta directa ({@code saveVentaDetalle}) y el
 * mismo abono ({@code registrarAbono}) que usa la pantalla. No se escribe ninguna tabla de pedidos a
 * mano: el stock, el precio del catalogo, las reglas del Apartado y del Ir pagando, el paso a
 * Pagado y la venta los pone el codigo de siempre.
 *
 * <p>R10: los clientes de prueba no tienen correo ni telefono y no se pide notificacion, asi que no
 * sale ningun correo ni WhatsApp.
 */
@Component
@RequiredArgsConstructor
public class PedidosPruebaVentaAdapter implements PedidosPruebaPort {

    static final String NOMBRE_CLIENTE = "Cliente Prueba QA";

    private final VentaServiceImpl ventas;
    private final IAbonoService abonos;
    private final IClienteSinRegistroRepository clientes;
    private final IPagosYMesesRepository pagosYMeses;
    private final JdbcTemplate jdbc;

    private Integer efectivoId;

    @Override
    public List<Integer> clientesDePrueba(int cuantos) {
        List<Integer> ids = new ArrayList<>(jdbc.queryForList(
                "SELECT id FROM clientes_sin_registro WHERE nombre_persona LIKE ? ORDER BY id",
                Integer.class, NOMBRE_CLIENTE + " %"));
        for (int n = ids.size() + 1; ids.size() < Math.max(1, cuantos); n++) {
            ClienteSinRegistro c = new ClienteSinRegistro();
            c.setNombrePersona(NOMBRE_CLIENTE + " " + String.format("%03d", n));
            c.setApeidoPaterno("Prueba");
            ids.add(clientes.save(c).getId());
        }
        return ids;
    }

    @Override
    public PedidoCreado crearPedido(TipoPedidoPrueba tipo, int clienteId, int usuarioId, List<LineaDePedido> lineas) {
        VentaDirectaRequest req = new VentaDirectaRequest();
        req.setUsuarioId(usuarioId);
        req.setClienteSinRegistroId(clienteId);
        req.setTipoPedido(tipo.tipoEnVenta());
        req.setObservaciones(MARCA_PEDIDO);
        if (tipo == TipoPedidoPrueba.CONTADO) {
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
            return new PedidoCreado(resp.getPedidoId(), resp.getTotalVenta() != null ? resp.getTotalVenta() : 0.0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void abonar(int pedidoId, double monto, int usuarioId) {
        AbonoRequest req = new AbonoRequest();
        req.setUsuarioId(usuarioId);
        req.setMonto(monto);
        req.setFechaPago(LocalDate.now());
        req.setMetodoPago("EFECTIVO");
        req.setNota("Abono de prueba");
        abonos.registrarAbono(pedidoId, req);
    }

    /** La forma de pago "Efectivo" (la misma que elige la pantalla); se busca una sola vez. */
    private Integer efectivo() {
        if (efectivoId == null) {
            efectivoId = pagosYMeses.findAll().stream()
                    .filter(p -> p.getTipoPago() != null && p.getTipoPago().getFormaPago() != null
                            && "efectivo".equalsIgnoreCase(p.getTipoPago().getFormaPago().trim()))
                    .map(PagosYMeses::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay forma de pago 'Efectivo' en pagos_y_meses: no se pueden crear ventas de contado"));
        }
        return efectivoId;
    }
}
