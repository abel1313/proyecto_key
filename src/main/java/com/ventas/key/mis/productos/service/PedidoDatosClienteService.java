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

    private static boolean vacio(String t) {
        return t == null || t.isBlank();
    }

    private static String limpio(String t) {
        return t == null ? "" : t.trim();
    }

    /**
     * ¿Este pedido no tiene los datos de un cliente? Cuando no tiene cliente, o esta a nombre de una cuenta de administrador, o su cliente sin registro
     * no tiene ni correo ni telefono (solo un nombre): no hay a donde mandarle su ticket ni su codigo.
     */
    public static boolean sinDatosDelCliente(Pedido p) {
        if (p == null) {
            return false;
        }
        if (p.getClienteSinRegistro() != null) {
            return vacio(p.getClienteSinRegistro().getCorreoElectronico()) && vacio(p.getClienteSinRegistro().getNumeroTelefonico());
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
        ClienteSinRegistro existente = pedido.getClienteSinRegistro();
        if (existente != null) {
            return completarClienteSinRegistro(pedidoId, pedido, existente, req, correo);
        }
        ClienteSinRegistroDto dto = ClienteSinRegistroDto.builder()
                .nombre_persona(req.getNombre() == null ? "" : req.getNombre().trim())
                .segundo_nombre(limpio(req.getSegundoNombre()))
                .apeido_Paterno(req.getApellido() == null ? "" : req.getApellido().trim())
                .apeido_Materno(limpio(req.getApellidoMaterno()))
                .fecha_Nacimiento(limpio(req.getFechaNacimiento()))
                .sexo(limpio(req.getSexo()))
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

    /** El pedido ya tiene un cliente sin registro (solo con nombre): se le completan el correo y el telefono, sin crear otro cliente. */
    private ClienteSinRegistro completarClienteSinRegistro(int pedidoId, Pedido pedido, ClienteSinRegistro csr, DatosClientePedidoRequest req, String correo) {
        String telefono = req.getTelefono() == null ? "" : req.getTelefono().trim();
        if (correo.isEmpty() && telefono.isEmpty()) {
            throw new IllegalArgumentException("Escribe al menos el correo o el teléfono del cliente");
        }
        // Si ya tiene nombre se conserva (lo que se escribe aqui no lo pisa); solo se usa el escrito cuando el cliente no tenia nombre.
        String nombre = vacio(csr.getNombrePersona()) ? (req.getNombre() == null ? "" : req.getNombre().trim()) : csr.getNombrePersona();
        String apellido = vacio(csr.getApeidoPaterno()) ? (req.getApellido() == null ? "" : req.getApellido().trim()) : csr.getApeidoPaterno();
        // Solo se validan lo nuevo (correo, telefono y los nombres opcionales que lleguen): el nombre que el cliente ya tenia se respeta tal cual (aunque no cumpla las 3 letras de un alta nueva).
        ClienteSinRegistroDto dto = ClienteSinRegistroDto.builder()
                .nombre_persona("Cliente").segundo_nombre(limpio(req.getSegundoNombre())).apeido_Paterno(vacio(csr.getApeidoPaterno()) ? apellido : "").apeido_Materno(limpio(req.getApellidoMaterno()))
                .fecha_Nacimiento("").sexo("").correo_Electronico(correo).numero_Telefonico(telefono).build();
        try {
            ClienteSinRegistroImpl.validar(dto);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        csr.setNombrePersona(nombre);
        csr.setApeidoPaterno(apellido);
        // Los campos opcionales que ahora trae el formulario completo: solo se llenan los que vengan con valor (no se borra lo que ya tenia).
        if (!limpio(req.getSegundoNombre()).isEmpty()) csr.setSegundoNombre(limpio(req.getSegundoNombre()));
        if (!limpio(req.getApellidoMaterno()).isEmpty()) csr.setApeidoMaterno(limpio(req.getApellidoMaterno()));
        if (!limpio(req.getSexo()).isEmpty()) csr.setSexo(limpio(req.getSexo()));
        if (!limpio(req.getFechaNacimiento()).isEmpty()) {
            ClienteSinRegistroDto f = ClienteSinRegistroDto.builder().nombre_persona("Cliente").segundo_nombre("").apeido_Paterno("").apeido_Materno("")
                    .fecha_Nacimiento(limpio(req.getFechaNacimiento())).sexo("").correo_Electronico("").numero_Telefonico("").build();
            try {
                ClienteSinRegistroImpl.validar(f);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            csr.setFechaNacimiento(java.time.LocalDate.parse(limpio(req.getFechaNacimiento())));
        }
        if (!correo.isEmpty()) {
            csr.setCorreoElectronico(correo);
            csr.setCorreoVerificado(false);
            csr.setCodigoVerificacion(null);
            csr.setCodigoVerificacionExpira(null);
        }
        if (!telefono.isEmpty()) {
            csr.setNumeroTelefonico(telefono);
        }
        clientesSinRegistro.guardarExistente(csr);
        if (campanas != null) {
            try {
                campanas.ligarClienteSinRegistroALaPersonaDelPedido(pedidoId, csr.getId());
            } catch (RuntimeException e) {
                log.warn("No se pudo ligar el cliente a la persona de campaña del pedido {}: {}", pedidoId, e.getMessage());
            }
        }
        log.info("Pedido {}: se completaron los datos del cliente sin registro {}", pedidoId, csr.getId());
        return csr;
    }

    /**
     * Corrige los datos del cliente de un pedido cuyo cliente es un «cliente sin registro» (el que se captura en Venta directa o desde el pedido). Los
     * mismos datos y validaciones que al darlo de alta. Un cliente con cuenta propia no se edita desde aqui: sus datos son suyos.
     */
    @Transactional
    public ClienteSinRegistro editar(int pedidoId, DatosClientePedidoRequest req) {
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(() -> new IllegalArgumentException("No se encontró el pedido"));
        if ("cancelado".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new IllegalArgumentException("El pedido está cancelado");
        }
        ClienteSinRegistro csr = pedido.getClienteSinRegistro();
        if (csr != null && !puedeEditarCliente(pedido)) {
            throw new IllegalArgumentException("Este pedido ya es de un cliente con cuenta propia: sus datos los cambia él desde su perfil");
        }
        if (csr == null) {
            throw new IllegalArgumentException(pedido.getCliente() != null && !sinDatosDelCliente(pedido)
                    ? "Este cliente tiene cuenta propia: sus datos los cambia él desde su perfil"
                    : "Este pedido todavía no tiene datos de cliente: usa «Agregar datos del cliente»");
        }
        String correo = limpio(req.getCorreo());
        if (!correo.isEmpty() && !ContactoCliente.correoValido(correo)) {
            throw new IllegalArgumentException("El correo no es válido. Revisa que tenga la forma nombre@dominio.com");
        }
        ClienteSinRegistroDto dto = ClienteSinRegistroDto.builder()
                .nombre_persona(limpio(req.getNombre())).segundo_nombre(limpio(req.getSegundoNombre()))
                .apeido_Paterno(limpio(req.getApellido())).apeido_Materno(limpio(req.getApellidoMaterno()))
                .fecha_Nacimiento(limpio(req.getFechaNacimiento())).sexo(limpio(req.getSexo()))
                .correo_Electronico(correo).numero_Telefonico(limpio(req.getTelefono())).build();
        java.time.LocalDate nacimiento;
        try {
            ClienteSinRegistroImpl.validar(dto);
            nacimiento = dto.getFecha_Nacimiento().isEmpty() ? null : java.time.LocalDate.parse(dto.getFecha_Nacimiento());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        boolean correoCambio = !correo.equalsIgnoreCase(limpio(csr.getCorreoElectronico()));
        csr.setNombrePersona(dto.getNombre_persona());
        csr.setSegundoNombre(dto.getSegundo_nombre());
        csr.setApeidoPaterno(dto.getApeido_Paterno());
        csr.setApeidoMaterno(dto.getApeido_Materno());
        csr.setFechaNacimiento(nacimiento);
        csr.setSexo(dto.getSexo());
        csr.setNumeroTelefonico(dto.getNumero_Telefonico());
        csr.setCorreoElectronico(correo);
        if (correoCambio) {
            csr.setCorreoVerificado(false);
            csr.setCodigoVerificacion(null);
            csr.setCodigoVerificacionExpira(null);
        }
        clientesSinRegistro.guardarExistente(csr);
        log.info("Pedido {}: se corrigieron los datos del cliente sin registro {}", pedidoId, csr.getId());
        return csr;
    }

    /**
     * Cual de los dos datos se muestra del pedido: el del cliente o el de quien recibe. Siempre tiene que haber uno: no se puede elegir uno que no tiene
     * nombre.
     */
    @Transactional
    public String cambiarDatosVisibles(int pedidoId, String mostrar) {
        String quien = mostrar == null ? "" : mostrar.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"CLIENTE".equals(quien) && !"RECEPTOR".equals(quien)) {
            throw new IllegalArgumentException("Elige mostrar los datos del cliente o los de quien recibe");
        }
        Pedido pedido = pedidos.findByIdWithLock(pedidoId).orElseThrow(() -> new IllegalArgumentException("No se encontró el pedido"));
        if ("cancelado".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new IllegalArgumentException("El pedido está cancelado");
        }
        if ("RECEPTOR".equals(quien) && vacio(pedido.getNombreReceptor())) {
            throw new IllegalArgumentException("Este pedido no tiene nombre de quien recibe: captúralo primero");
        }
        if ("CLIENTE".equals(quien) && !tieneDatosDeCliente(pedido)) {
            throw new IllegalArgumentException("Este pedido no tiene datos de cliente: agrégalos primero");
        }
        pedido.setDatosVisibles(quien);
        pedidos.save(pedido);
        log.info("Pedido {}: se muestran los datos de {}", pedidoId, quien);
        return quien;
    }

    /** El cliente sin registro del pedido se puede corregir: no hay una cuenta de cliente (que no sea la de quien cobra) que sea la dueña del pedido. */
    public static boolean puedeEditarCliente(Pedido p) {
        return p != null && p.getClienteSinRegistro() != null && (p.getCliente() == null || clienteEsCuentaDeAdmin(p));
    }

    public static boolean clienteEsCuentaDeAdmin(Pedido p) {
        var usuario = p.getCliente() == null ? null : p.getCliente().getUsuario();
        return usuario != null && usuario.getRoles() != null && "ROLE_ADMIN".equals(usuario.getRoles().getNombreRol());
    }

    /** ¿Hay un cliente real con nombre (no la cuenta del administrador que cobro)? */
    public static boolean tieneDatosDeCliente(Pedido p) {
        if (p.getClienteSinRegistro() != null) {
            return !vacio(p.getClienteSinRegistro().getNombrePersona());
        }
        if (p.getCliente() == null) {
            return false;
        }
        var usuario = p.getCliente().getUsuario();
        boolean admin = usuario != null && usuario.getRoles() != null && "ROLE_ADMIN".equals(usuario.getRoles().getNombreRol());
        return !admin && !vacio(p.getCliente().getNombrePersona());
    }
}
