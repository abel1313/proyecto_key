package com.ventas.key.mis.productos.service;

import com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.pedidos.DatosClientePedidoRequest;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrega los datos del cliente a un pedido que no tiene cliente: los pedidos de mostrador sin datos quedan a nombre de la cuenta de quien vende (o sin
 * cliente). Se crea un «cliente sin registro» con los datos escritos y se liga al pedido (y a su venta, si ya la tiene). Si el pedido ya tiene
 * un codigo de campana (persona sin cliente), esa persona queda ligada al nuevo cliente.
 */
@Service
@Slf4j
public class PedidoDatosClienteService {

    private final IPedidoRepository pedidos;
    private final IVentaRepository ventas;
    private final ClienteSinRegistroImpl clientesSinRegistro;

    @Autowired(required = false)
    private ParticiparEnCampanaCasoUso campanas;

    public PedidoDatosClienteService(IPedidoRepository pedidos, IVentaRepository ventas, ClienteSinRegistroImpl clientesSinRegistro) {
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.clientesSinRegistro = clientesSinRegistro;
    }

    /** ¿Este pedido no tiene los datos de un cliente? (sin cliente, o a nombre de una cuenta de administrador, o sin registro vacio). */
    public static boolean sinDatosDelCliente(Pedido p) {
        if (p == null || p.getClienteSinRegistro() != null) {
            return false;
        }
        if (p.getCliente() == null) {
            return true;
        }
        var usuario = p.getCliente().getUsuario();
        return usuario != null && usuario.getRoles() != null && "ROLE_ADMIN".equals(usuario.getRoles().getNombreRol());
    }

    @Transactional
    public ClienteSinRegistro agregar(int pedidoId, DatosClientePedidoRequest req) {
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(() -> new IllegalArgumentException("No se encontró el pedido"));
        if ("cancelado".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new IllegalArgumentException("El pedido está cancelado");
        }
        if (!sinDatosDelCliente(pedido)) {
            throw new IllegalArgumentException("Este pedido ya tiene los datos de su cliente");
        }
        String correo = req.getCorreo() == null ? "" : req.getCorreo().trim();
        if (!correo.isEmpty() && !ContactoCliente.correoValido(correo)) {
            throw new IllegalArgumentException("El correo no es válido. Revisa que tenga la forma nombre@dominio.com");
        }
        ClienteSinRegistroDto dto = ClienteSinRegistroDto.builder()
                .nombre_persona(req.getNombre() == null ? "" : req.getNombre().trim())
                .segundo_nombre("")
                .apeido_Paterno(req.getApellido() == null ? "" : req.getApellido().trim())
                .apeido_Materno("")
                .fecha_Nacimiento("")
                .sexo("")
                .correo_Electronico(correo)
                .numero_Telefonico(req.getTelefono() == null ? "" : req.getTelefono().trim())
                .build();
        ClienteSinRegistro csr;
        try {
            csr = clientesSinRegistro.crear(dto);    // valida nombre (3 letras), correo y telefono de 10 digitos
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        pedido.setCliente(null);
        pedido.setClienteSinRegistro(csr);
        pedidos.save(pedido);
        ventas.findByPedidoId(pedidoId).ifPresent(v -> {
            v.setCliente(null);
            v.setClienteSinRegistro(csr);
            ventas.save(v);
        });
        if (campanas != null) {
            try {
                campanas.ligarClienteSinRegistroALaPersonaDelPedido(pedidoId, csr.getId());
            } catch (RuntimeException e) {
                log.warn("No se pudo ligar el cliente nuevo a la persona de campaña del pedido {}: {}", pedidoId, e.getMessage());
            }
        }
        log.info("Pedido {}: se agregaron los datos del cliente (cliente sin registro {})", pedidoId, csr.getId());
        return csr;
    }
}
