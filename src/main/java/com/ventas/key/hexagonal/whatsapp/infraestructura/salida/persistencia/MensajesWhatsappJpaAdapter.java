package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DireccionMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
import com.ventas.key.mis.productos.entity.WhatsappMensajeEntidad;
import com.ventas.key.mis.productos.repository.IWhatsappMensajeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda el historial de WhatsApp en MySQL. Fechas en UTC. */
@Component
public class MensajesWhatsappJpaAdapter implements MensajesWhatsappPort {

    private final IWhatsappMensajeRepository repositorio;

    public MensajesWhatsappJpaAdapter(IWhatsappMensajeRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Sin @Transactional a propósito (ver ClientesWhatsappJpaAdapter.buscarOCrear): el reenvío que
    // llega a la vez choca con la llave única de wa_message_id y se atrapa aquí.
    @Override
    public boolean guardarEntrante(long clienteId, MensajeEntrante m) {
        if (repositorio.existsByWaMessageId(m.waMessageId())) {
            return false;
        }
        LocalDateTime ahora = ClientesWhatsappJpaAdapter.aUtc(Instant.now());
        WhatsappMensajeEntidad e = WhatsappMensajeEntidad.builder()
                .clienteId(clienteId).waMessageId(m.waMessageId())
                .direccion(DireccionMensaje.ENTRANTE.name()).origen(OrigenMensaje.CLIENTE.name())
                .tipo(recortar(m.tipo() == null ? "text" : m.tipo(), 20)).contenido(m.texto())
                .estado(EstadoMensaje.RECIBIDO.name())
                .enviadoEn(m.enviadoEn() != null ? ClientesWhatsappJpaAdapter.aUtc(m.enviadoEn()) : ahora)
                .creadoAt(ahora).build();
        try {
            repositorio.saveAndFlush(e);
            return true;
        } catch (DataIntegrityViolationException duplicado) {
            return false;
        }
    }

    @Override
    @Transactional
    public MensajeWhatsapp guardarSaliente(long clienteId, OrigenMensaje origen, String tipo, String contenido,
                                           EstadoMensaje estado, String waMessageId, String motivo,
                                           Integer codigoError, String plantilla, Instant en) {
        WhatsappMensajeEntidad e = WhatsappMensajeEntidad.builder()
                .clienteId(clienteId).waMessageId(waMessageId)
                .direccion(DireccionMensaje.SALIENTE.name()).origen(origen.name())
                .tipo(recortar(tipo, 20)).contenido(contenido).estado(estado.name())
                .motivo(recortar(motivo, 255)).codigoError(codigoError).plantilla(recortar(plantilla, 100))
                .enviadoEn(ClientesWhatsappJpaAdapter.aUtc(en)).creadoAt(ClientesWhatsappJpaAdapter.aUtc(en)).build();
        return aDominio(repositorio.save(e));
    }

    @Override
    @Transactional
    public Optional<Long> actualizarEstadoPorWaId(EstadoDeEntrega aviso) {
        return repositorio.findByWaMessageId(aviso.waMessageId()).map(e -> {
            EstadoMensaje actual = EstadoMensaje.valueOf(e.getEstado());
            if (actual.puedeCambiarA(aviso.estado())) {
                e.setEstado(aviso.estado().name());
                if (aviso.estado() == EstadoMensaje.FALLIDO) {
                    e.setCodigoError(aviso.codigoError());
                    e.setMotivo(recortar(aviso.motivo(), 255));
                }
            }
            return e.getClienteId();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<MensajeWhatsapp> historial(long clienteId, int pagina, int size) {
        Page<WhatsappMensajeEntidad> p = repositorio.findByClienteIdOrderByIdDesc(clienteId, PageRequest.of(pagina, size));
        return new Pagina<>(p.getContent().stream().map(MensajesWhatsappJpaAdapter::aDominio).toList(),
                pagina, size, p.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeWhatsapp> ultimos(long clienteId, int cuantos) {
        List<MensajeWhatsapp> recientes = new ArrayList<>(
                repositorio.findByClienteIdOrderByIdDesc(clienteId, PageRequest.of(0, cuantos)).getContent().stream()
                        .map(MensajesWhatsappJpaAdapter::aDominio).toList());
        Collections.reverse(recientes);
        return recientes;
    }

    @Override
    @Transactional(readOnly = true)
    public long contarSinResponder(long clienteId) {
        return repositorio.contarEntrantesSinResponder(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean yaLeHabiamosEscrito(long clienteId) {
        return repositorio.existsByClienteIdAndDireccionAndEstadoIn(clienteId, DireccionMensaje.SALIENTE.name(),
                List.of(EstadoMensaje.ENVIADO.name(), EstadoMensaje.ENTREGADO.name(), EstadoMensaje.LEIDO.name()));
    }

    static MensajeWhatsapp aDominio(WhatsappMensajeEntidad e) {
        return new MensajeWhatsapp(e.getId(), e.getClienteId(), e.getWaMessageId(),
                DireccionMensaje.valueOf(e.getDireccion()), OrigenMensaje.valueOf(e.getOrigen()), e.getTipo(),
                e.getContenido(), EstadoMensaje.valueOf(e.getEstado()), e.getMotivo(), e.getCodigoError(),
                e.getPlantilla(), ClientesWhatsappJpaAdapter.aInstant(e.getEnviadoEn()));
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
