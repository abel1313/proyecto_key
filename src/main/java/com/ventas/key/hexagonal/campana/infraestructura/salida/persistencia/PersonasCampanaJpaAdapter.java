package com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.CampanaPersonaEntidad;
import com.ventas.key.mis.productos.entity.CampanaPersonaPedidoEntidad;
import com.ventas.key.mis.productos.repository.ICampanaPersonaPedidoRepository;
import com.ventas.key.mis.productos.repository.ICampanaPersonaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.hexagonal.campana.dominio.modelo.ResumenDePersona;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/** [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Guarda personas y lo que suman en MySQL. Sin la migración, {@code disponible()} es falso. */
@Component
public class PersonasCampanaJpaAdapter implements PersonasCampanaPort {

    private final ICampanaPersonaRepository personas;
    private final ICampanaPersonaPedidoRepository pedidos;
    private final TablasDisponibles tablas;

    public PersonasCampanaJpaAdapter(ICampanaPersonaRepository personas, ICampanaPersonaPedidoRepository pedidos, TablasDisponibles tablas) {
        this.personas = personas;
        this.pedidos = pedidos;
        this.tablas = tablas;
    }

    @Override
    public boolean disponible() {
        return tablas.existe("campana_persona") && tablas.existe("campana_persona_pedido");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaCampana> porUuid(String uuid) {
        return personas.findByUuid(uuid).map(PersonasCampanaJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaCampana> porContacto(String correo, String telefono10) {
        if (correo != null && !correo.isBlank()) {
            Optional<CampanaPersonaEntidad> p = personas.findFirstByCorreoIgnoreCase(correo);
            if (p.isPresent()) {
                return p.map(PersonasCampanaJpaAdapter::aDominio);
            }
        }
        if (telefono10 != null && telefono10.length() == 10) {
            return personas.findFirstByTelefono(telefono10).map(PersonasCampanaJpaAdapter::aDominio);
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public PersonaCampana guardar(PersonaCampana p) {
        CampanaPersonaEntidad e = p.id() == null ? new CampanaPersonaEntidad()
                : personas.findById(p.id()).orElseGet(CampanaPersonaEntidad::new);
        if (e.getId() == null) {
            e.setFechaCreacion(LocalDateTime.now());
        }
        e.setUuid(p.uuid());
        e.setNombre(p.nombre());
        e.setCorreo(p.correo());
        e.setTelefono(p.telefono());
        e.setConsentimiento(p.consentimiento());
        return aDominio(personas.saveAndFlush(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal acumulado(int campanaId, int personaId) {
        BigDecimal suma = pedidos.acumulado(campanaId, personaId);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    @Override
    @Transactional
    public void ligarPedido(int campanaId, int personaId, int pedidoId, BigDecimal monto) {
        CampanaPersonaPedidoEntidad e = new CampanaPersonaPedidoEntidad();
        e.setCampanaId(campanaId);
        e.setPersonaId(personaId);
        e.setPedidoId(pedidoId);
        e.setMonto(monto == null ? BigDecimal.ZERO : monto);
        pedidos.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaCampana> dePedido(int pedidoId) {
        return pedidos.personaDelPedido(pedidoId).stream().findFirst()
                .flatMap(personas::findById).map(PersonasCampanaJpaAdapter::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenDePersona> resumen(int campanaId) {
        return pedidos.resumenPorPersona(campanaId).stream()
                .map(f -> new ResumenDePersona(
                        personas.findById((Integer) f[0]).map(PersonasCampanaJpaAdapter::aDominio).orElseThrow(),
                        (BigDecimal) f[1], ((Number) f[2]).longValue()))
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(int personaId) {
        pedidos.borrarDePersona(personaId);
        personas.deleteById(personaId);
    }

    private static PersonaCampana aDominio(CampanaPersonaEntidad e) {
        return new PersonaCampana(e.getId(), e.getUuid(), e.getNombre(), e.getCorreo(), e.getTelefono(), e.isConsentimiento());
    }
}
