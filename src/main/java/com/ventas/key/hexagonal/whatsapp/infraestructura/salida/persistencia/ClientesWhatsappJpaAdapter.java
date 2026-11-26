package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.mis.productos.entity.WhatsappClienteEntidad;
import com.ventas.key.mis.productos.repository.IWhatsappClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda los clientes de WhatsApp en MySQL. Fechas en UTC. */
@Component
public class ClientesWhatsappJpaAdapter implements ClientesWhatsappPort {

    private final IWhatsappClienteRepository repositorio;

    public ClientesWhatsappJpaAdapter(IWhatsappClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Sin @Transactional a propósito: si dos reenvíos crean al mismo cliente a la vez, el segundo
    // choca con la llave única y aquí se atrapa; dentro de una transacción ya habría quedado marcada
    // para deshacerse.
    @Override
    public ClienteWhatsapp buscarOCrear(String telefono, String nombre, Instant ahora) {
        Optional<WhatsappClienteEntidad> existente = repositorio.findByTelefono(telefono);
        if (existente.isPresent()) {
            WhatsappClienteEntidad e = existente.get();
            String nuevo = recortar(nombre, 100);
            if (nuevo != null && !nuevo.isBlank() && !nuevo.equals(e.getNombre())) {
                e.setNombre(nuevo);
                e = repositorio.save(e);
            }
            return aDominio(e);
        }
        LocalDateTime ahoraUtc = aUtc(ahora);
        WhatsappClienteEntidad nuevo = WhatsappClienteEntidad.builder()
                .telefono(telefono).nombre(recortar(nombre, 100))
                .ultimaActividadAt(ahoraUtc).creadoAt(ahoraUtc).build();
        try {
            return aDominio(repositorio.saveAndFlush(nuevo));
        } catch (DataIntegrityViolationException duplicado) {
            return aDominio(repositorio.findByTelefono(telefono).orElseThrow(() -> duplicado));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteWhatsapp> porId(long id) {
        return repositorio.findById(id).map(ClientesWhatsappJpaAdapter::aDominio);
    }

    @Override
    @Transactional
    public void registrarEntrante(long id, Instant enviadoEn, Instant ahora, String resumen) {
        repositorio.findById(id).ifPresent(e -> {
            LocalDateTime marca = aUtc(enviadoEn);
            // Nunca hacia atrás: si Meta entrega dos mensajes en desorden, gana el más reciente.
            if (e.getUltimoEntranteAt() == null || e.getUltimoEntranteAt().isBefore(marca)) {
                e.setUltimoEntranteAt(marca);
            }
            e.setUltimaActividadAt(aUtc(ahora));
            e.setUltimoMensaje(recortar(resumen, 200));
        });
    }

    @Override
    @Transactional
    public void registrarSaliente(long id, String resumen, Instant ahora) {
        repositorio.findById(id).ifPresent(e -> {
            e.setUltimaActividadAt(aUtc(ahora));
            e.setUltimoMensaje(recortar(resumen, 200));
        });
    }

    @Override
    @Transactional
    public void pausarBot(long id, Instant desde) {
        repositorio.findById(id).ifPresent(e -> e.setPausaBotDesde(aUtc(desde)));
    }

    @Override
    @Transactional
    public void tomarConversacion(long id, Instant desde) {
        repositorio.findById(id).ifPresent(e -> {
            e.setBotFijoHumano(true);
            e.setPausaBotDesde(aUtc(desde));
        });
    }

    @Override
    @Transactional
    public void reanudarBot(long id) {
        repositorio.findById(id).ifPresent(e -> {
            e.setBotFijoHumano(false);
            e.setPausaBotDesde(null);
        });
    }

    @Override
    @Transactional
    public void marcarAtencion(long id, boolean requiere) {
        repositorio.findById(id).ifPresent(e -> e.setRequiereAtencion(requiere));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ClienteWhatsapp> listar(int pagina, int size) {
        Page<WhatsappClienteEntidad> p = repositorio.findAllByOrderByUltimaActividadAtDescIdDesc(PageRequest.of(pagina, size));
        return new Pagina<>(p.getContent().stream().map(ClientesWhatsappJpaAdapter::aDominio).toList(),
                pagina, size, p.getTotalElements());
    }

    static ClienteWhatsapp aDominio(WhatsappClienteEntidad e) {
        return new ClienteWhatsapp(e.getId(), e.getTelefono(), e.getNombre(), aInstant(e.getUltimoEntranteAt()),
                aInstant(e.getUltimaActividadAt()), aInstant(e.getPausaBotDesde()), e.isBotFijoHumano(),
                e.isRequiereAtencion(), e.getUltimoMensaje(), aInstant(e.getCreadoAt()));
    }

    static LocalDateTime aUtc(Instant i) {
        return i == null ? null : LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    static Instant aInstant(LocalDateTime l) {
        return l == null ? null : l.toInstant(ZoneOffset.UTC);
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
