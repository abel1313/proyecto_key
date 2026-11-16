package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.DividirPedidoCasoUso.DestinoCliente;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.DivisionDePedidoPort;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.repository.IGrupoPedidoMiembroRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/** [Hexagonal: Driven Adapter] Crea el pedido nuevo de una division y contesta lo que el caso de uso necesita saber de la base. */
@Component
public class DivisionDePedidoJpaAdapter implements DivisionDePedidoPort {

    private final IPedidoRepository pedidos;
    private final IClienteRepository clientes;
    private final IClienteSinRegistroRepository clientesSinRegistro;
    private final IGrupoPedidoMiembroRepository grupos;

    public DivisionDePedidoJpaAdapter(IPedidoRepository pedidos, IClienteRepository clientes,
                                      IClienteSinRegistroRepository clientesSinRegistro, IGrupoPedidoMiembroRepository grupos) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.clientesSinRegistro = clientesSinRegistro;
        this.grupos = grupos;
    }

    @Override
    @Transactional
    public void bloquear(Integer pedidoId) {
        pedidos.findByIdWithLock(pedidoId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaEnGrupoActivo(Integer pedidoId) {
        return grupos.estaEnGrupoActivo(pedidoId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean destinoValido(Integer pedidoOrigenId, DestinoCliente destino) {
        Pedido origen = pedidos.findById(pedidoOrigenId).orElse(null);
        if (origen == null || destino == null || !destino.esValido()) {
            return false;
        }
        if (destino.clienteId() != null) {
            boolean existe = clientes.existsById(destino.clienteId());
            boolean mismo = origen.getCliente() != null && destino.clienteId().equals(origen.getCliente().getId());
            return existe && !mismo;
        }
        boolean existe = clientesSinRegistro.existsById(destino.clienteSinRegistroId());
        boolean mismo = origen.getClienteSinRegistro() != null && destino.clienteSinRegistroId().equals(origen.getClienteSinRegistro().getId());
        return existe && !mismo;
    }

    @Override
    @Transactional
    public int crearPedidoParaCliente(Integer pedidoOrigenId, DestinoCliente destino, String nota) {
        Pedido origen = pedidos.findById(pedidoOrigenId)
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado: " + pedidoOrigenId));
        Pedido nuevo = new Pedido();
        if (destino.clienteId() != null) {
            nuevo.setCliente(clientes.getReferenceById(destino.clienteId()));
        } else {
            nuevo.setClienteSinRegistro(clientesSinRegistro.getReferenceById(destino.clienteSinRegistroId()));
        }
        nuevo.setTipoPedido(origen.getTipoPedido());
        nuevo.setEstadoPedido(origen.getTipoPedido());       // sin abonos: queda en su forma de cobro (Apartado / Ir pagando)
        nuevo.setFechaPedido(LocalDate.now());
        nuevo.setFechaHoraRegistro(LocalDateTime.now());
        nuevo.setObservaciones(nota);
        nuevo.setTotalPedido(0.0);
        nuevo.setTotalPagado(0.0);
        nuevo.marcarEntregado(origen.isEntregado());          // si el cliente de origen ya se llevo lo suyo, lo movido se trata igual
        nuevo.setDetalles(new ArrayList<>());
        return pedidos.save(nuevo).getId();
    }
}
