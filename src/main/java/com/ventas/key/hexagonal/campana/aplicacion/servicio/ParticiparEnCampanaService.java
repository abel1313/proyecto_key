package com.ventas.key.hexagonal.campana.aplicacion.servicio;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ClienteDeCodigo;
import com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente;
import com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ReglasCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.VerificacionDeCodigo;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarCampanaActivaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.AvisarCodigoCampanaPort;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * [Clean: Use Case] La campaña vigente frente a una venta: decide los precios de campaña, registra a la persona al cerrar la venta y verifica
 * códigos. Java puro. Reglas: el código solo vale si es de ESE cliente (correo o teléfono); la meta suma lo acumulado más lo del ticket sin los
 * juguetes de la campaña; solo cuentan las compras de Contado (pagadas); a quien compra con sus datos y acepta el aviso de privacidad se le crea
 * su código.
 */
public class ParticiparEnCampanaService implements ParticiparEnCampanaCasoUso {

    private final ConsultarCampanaActivaCasoUso activa;
    private final PersonasCampanaPort personas;
    private final AvisarCodigoCampanaPort aviso;
    private final LimiteDeIntentos limite;
    private final Supplier<String> nuevoCodigo;

    public ParticiparEnCampanaService(ConsultarCampanaActivaCasoUso activa, PersonasCampanaPort personas, AvisarCodigoCampanaPort aviso,
                                      LimiteDeIntentos limite, Supplier<String> nuevoCodigo) {
        this.activa = activa;
        this.personas = personas;
        this.aviso = aviso;
        this.limite = limite;
        this.nuevoCodigo = nuevoCodigo;
    }

    @Override
    public DecisionDeCampana decidir(SolicitudDeCampana s) {
        if (!personas.disponible()) {
            return DecisionDeCampana.NINGUNA;
        }
        Optional<Campana> vigente = activa.vigente();
        if (vigente.isEmpty()) {
            return DecisionDeCampana.NINGUNA;
        }
        Campana campana = vigente.get();

        // El código vale solo si es de este cliente; uno ajeno o inexistente se ignora (se cobra normal).
        PersonaCampana persona = null;
        if (s.codigo() != null && !s.codigo().isBlank()) {
            persona = personas.porUuid(s.codigo().trim().toLowerCase()).filter(p -> p.coincideCon(s.contacto())).orElse(null);
        }

        // "Sin juguetes": solo sirve para lo que se anota en la liga del pedido. Para la meta TODO suma (juguetes incluidos).
        BigDecimal sinJuguetes = BigDecimal.ZERO;
        BigDecimal totalTicket = BigDecimal.ZERO;
        for (SolicitudDeCampana.Linea l : s.lineas()) {
            BigDecimal importe = l.precioBase().multiply(BigDecimal.valueOf(l.cantidad()));
            totalTicket = totalTicket.add(importe);
            if (campana.precioDe(l.productoId()).isEmpty()) {
                sinJuguetes = sinJuguetes.add(importe);
            }
        }
        // Lo que lleva de la campaña = lo PAGADO entre su fecha de inicio y de fin. Lo de este ticket solo cuenta si se paga ahora (Contado).
        BigDecimal acumulado = persona == null ? BigDecimal.ZERO : personas.acumulado(campana, persona);
        BigDecimal ticketQueSuma = s.cobro() == FormaDeCobro.CONTADO ? totalTicket : BigDecimal.ZERO;
        boolean recurrente = persona != null
                ? ReglasCampana.califica(acumulado, ticketQueSuma, campana.metaMonto(), s.recurrenteManual())
                : s.recurrenteManual();

        Map<Integer, BigDecimal> precios = new HashMap<>();
        for (SolicitudDeCampana.Linea l : s.lineas()) {
            Optional<PrecioDeCampana> pc = campana.precioDe(l.productoId());
            if (pc.isPresent()) {
                BigDecimal precio = ReglasCampana.precioParaLinea(s.cobro(), recurrente, l.precioBase(), pc.get().precio());
                if (precio.compareTo(l.precioBase()) < 0) {
                    precios.put(l.productoId(), precio);
                }
            }
        }

        // Quien compra con sus datos y acepta el aviso de privacidad tiene (o recibe) su código, para acumular hacia la meta.
        // Con la casilla «Cliente recurrente» marcada a mano NO se crea persona ni QR (a quien ya se conoce no hace falta); si ya existía
        // una persona con esos datos, solo se liga la compra a ella.
        PersonaCampana candidata = null;
        if (persona == null && s.aceptaPrivacidad() && s.contacto() != null && s.contacto().tieneDatos()) {
            Optional<PersonaCampana> existente = personas.porContacto(s.contacto().correo(), s.contacto().telefono());
            if (existente.isPresent()) {
                persona = existente.get();
            } else if (!s.recurrenteManual()) {
                candidata = new PersonaCampana(null, nuevoCodigo.get(), s.contacto().nombre(), s.contacto().correo(),
                        s.contacto().telefono(), true, s.contacto().clienteId(), s.contacto().clienteSinRegistroId());
            }
        }
        return new DecisionDeCampana(campana.id(), campana.nombre(), precios, persona, candidata, sinJuguetes, recurrente,
                s.contacto() == null ? null : s.contacto().clienteId(), s.contacto() == null ? null : s.contacto().clienteSinRegistroId(),
                s.contacto() == null ? null : s.contacto().correo());
    }

    @Override
    public void registrarCompra(DecisionDeCampana d, int pedidoId, FormaDeCobro cobro) {
        if (d == null || !d.hayCampana()) {
            return;
        }
        PersonaCampana persona = d.persona();
        boolean nueva = false;
        if (persona == null && d.candidata() != null) {
            persona = personas.guardar(d.candidata());
            nueva = true;
        }
        if (persona == null) {
            return;
        }
        if (!nueva) {
            // Una persona anterior al ligado con el cliente: se le anota el cliente de esta compra (si todavía no lo tenía).
            personas.completarCliente(persona.id(), d.clienteId(), d.clienteSinRegistroId());
        }
        BigDecimal monto = cobro == FormaDeCobro.CONTADO ? d.ticketSinJuguetes() : BigDecimal.ZERO;
        personas.ligarPedido(d.campanaId(), persona.id(), pedidoId, monto);
        // El código sale en el correo de CADA compra (no solo la primera): así la persona siempre lo tiene a la mano. Va al correo de la venta,
        // y si no trae, al de la persona; sin ninguno no se manda nada (el código existe igual y el administrador lo ve).
        String destino = d.correoDeLaVenta() != null && !d.correoDeLaVenta().isBlank() ? d.correoDeLaVenta().trim() : persona.correo();
        if (destino != null && !destino.isBlank()) {
            try {
                aviso.enviar(persona, d.nombreCampana(), destino, nueva);
            } catch (RuntimeException e) {
                // El correo nunca hace caer la venta; el código existe igual y el administrador lo ve.
            }
        }
    }

    @Override
    public VerificacionDeCodigo verificar(String quien, String codigo, ContactoCliente contacto) {
        limite.registrar(quien);
        Optional<Campana> vigente = personas.disponible() ? activa.vigente() : Optional.empty();
        BigDecimal meta = vigente.map(Campana::metaMonto).orElse(BigDecimal.ZERO);
        if (codigo == null || codigo.isBlank()) {
            return new VerificacionDeCodigo(VerificacionDeCodigo.Estado.NO_EXISTE, "", BigDecimal.ZERO, meta);
        }
        Optional<PersonaCampana> p = personas.disponible() ? personas.porUuid(codigo.trim().toLowerCase()) : Optional.empty();
        if (p.isEmpty()) {
            return new VerificacionDeCodigo(VerificacionDeCodigo.Estado.NO_EXISTE, "", BigDecimal.ZERO, meta);
        }
        if (!p.get().coincideCon(contacto)) {
            return new VerificacionDeCodigo(VerificacionDeCodigo.Estado.NO_COINCIDE, "", BigDecimal.ZERO, meta);
        }
        BigDecimal acumulado = vigente.isPresent() ? personas.acumulado(vigente.get(), p.get()) : BigDecimal.ZERO;
        return new VerificacionDeCodigo(VerificacionDeCodigo.Estado.OK, p.get().nombre(), acumulado, meta);
    }

    @Override
    public ClienteDeCodigo buscarPorCodigo(String quien, String codigo) {
        limite.registrar(quien);
        if (codigo == null || codigo.isBlank() || !personas.disponible()) {
            return ClienteDeCodigo.invalido();
        }
        Optional<PersonaCampana> p = personas.porUuid(codigo.trim().toLowerCase());
        if (p.isEmpty()) {
            return ClienteDeCodigo.invalido();
        }
        Optional<Campana> vigente = activa.vigente();
        BigDecimal acumulado = vigente.isPresent() ? personas.acumulado(vigente.get(), p.get()) : BigDecimal.ZERO;
        BigDecimal meta = vigente.map(Campana::metaMonto).orElse(BigDecimal.ZERO);
        return new ClienteDeCodigo(true, p.get(), acumulado, meta);
    }

    @Override
    public void eliminarPersona(int personaId) {
        personas.eliminar(personaId);
    }

    @Override
    public boolean pedidoTieneCodigo(int pedidoId) {
        return personas.disponible() && personas.dePedido(pedidoId).isPresent();
    }

    @Override
    public ResultadoReenvio reenviarCodigo(int pedidoId) {
        Optional<PersonaCampana> persona = personas.disponible() ? personas.dePedido(pedidoId) : Optional.empty();
        if (persona.isEmpty()) {
            return ResultadoReenvio.SIN_CODIGO;
        }
        if (persona.get().correo() == null) {
            return ResultadoReenvio.SIN_CORREO;
        }
        aviso.enviar(persona.get(), activa.vigente().map(Campana::nombre).orElse(null), persona.get().correo(), false);
        return ResultadoReenvio.ENVIADO;
    }
}
