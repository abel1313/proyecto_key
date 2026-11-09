package com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.CampanaPersonaEntidad;
import com.ventas.key.mis.productos.entity.CampanaPersonaPedidoEntidad;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.mis.productos.repository.ICampanaPersonaPedidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final com.ventas.key.mis.productos.repository.ICampanaRepository campanas;

    @PersistenceContext
    private EntityManager em;

    public PersonasCampanaJpaAdapter(ICampanaPersonaRepository personas, ICampanaPersonaPedidoRepository pedidos, TablasDisponibles tablas,
                                     com.ventas.key.mis.productos.repository.ICampanaRepository campanas) {
        this.campanas = campanas;
        this.personas = personas;
        this.pedidos = pedidos;
        this.tablas = tablas;
    }

    @Override
    public boolean disponible() {
        // Incluye las columnas de migration_campana_persona_cliente.sql: sin ella la entidad no se puede leer y la campana se apaga sin romper la venta.
        return tablas.existe("campana_persona") && tablas.existe("campana_persona_pedido")
                && tablas.existeColumna("campana_persona", "cliente_id") && tablas.existeColumna("campana_persona", "cliente_sin_registro_id");
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
        e.setClienteId(p.clienteId());
        e.setClienteSinRegistroId(p.clienteSinRegistroId());
        return aDominio(personas.saveAndFlush(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal acumulado(int campanaId, int personaId) {
        BigDecimal suma = pedidos.acumulado(campanaId, personaId);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    /** Los pedidos de la persona: los ligados a ella más los de su cliente (registrado o sin registro). */
    private static final String PEDIDOS_DE_LA_PERSONA =
            "(p.id IN (SELECT x.pedido_id FROM campana_persona_pedido x WHERE x.persona_id = :persona) "
          + "OR (:cliente IS NOT NULL AND p.cliente_id = :cliente) "
          + "OR (:sinRegistro IS NOT NULL AND p.cliente_sin_registro_id = :sinRegistro))";

    /**
     * Lo PAGADO por la persona entre la fecha de inicio y la de fin de la campaña (los dos días incluidos). Todo suma, juguetes incluidos. Lo anterior
     * a la fecha de inicio no cuenta. Contado: el total de su venta (no devuelta); Apartado e Ir pagando: lo abonado dentro del rango. Un pedido
     * cancelado o pendiente (tarjeta sin aprobar) no suma. Se calcula al vuelo: no hay un monto guardado que se desfase al abonar o devolver.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal acumulado(Campana campana, PersonaCampana persona) {
        if (campana == null || persona == null || persona.id() == null) {
            return BigDecimal.ZERO;
        }
        java.time.LocalDateTime desde = campana.fechaInicio().atStartOfDay();
        java.time.LocalDateTime hastaExcl = campana.fechaFin().plusDays(1).atStartOfDay();

        BigDecimal contado = suma(em.createNativeQuery(
                // LEAST(venta, total del pedido hoy): si se devolvió UN artículo de un pedido ya pagado, el pedido baja su total pero la venta conserva su
                // monto; con el tope, el acumulado baja lo devuelto. Si el pedido creció después de pagar, solo cuenta lo que se pagó (el monto de la venta).
                "SELECT COALESCE(SUM(LEAST(v.total_venta, p.total_pedido)), 0) FROM ventas v JOIN pedidos p ON p.id = v.pedido_id "
              + "WHERE " + PEDIDOS_DE_LA_PERSONA + " AND p.tipo_pedido NOT IN ('APARTADO','FIADO') "
              + "AND LOWER(p.estado_pedido) NOT IN ('pendiente','cancelado') AND v.estado_venta <> 'Devuelta' "
              + "AND v.fecha_venta >= :desde AND v.fecha_venta < :hasta"), persona, desde, hastaExcl);

        BigDecimal credito = suma(em.createNativeQuery(
                // Por pedido: lo abonado, sin pasar del total que el pedido tiene hoy (si se devolvió un artículo, lo abonado de más es saldo a favor, no compra).
                "SELECT COALESCE(SUM(LEAST(t.abonado, t.total)), 0) FROM ("
              + "SELECT p.id AS pedido, p.total_pedido AS total, SUM(a.monto) AS abonado FROM abono_pedido a JOIN pedidos p ON p.id = a.pedido_id "
              + "WHERE " + PEDIDOS_DE_LA_PERSONA + " AND p.tipo_pedido IN ('APARTADO','FIADO') "
              + "AND LOWER(p.estado_pedido) <> 'cancelado' AND a.fecha_pago >= :desde AND a.fecha_pago < :hasta "
              + "GROUP BY p.id, p.total_pedido) t"), persona,
                campana.fechaInicio().atStartOfDay(), hastaExcl);
        return contado.add(credito).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal suma(jakarta.persistence.Query consulta, PersonaCampana persona, java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        consulta.setParameter("persona", persona.id());
        consulta.setParameter("cliente", persona.clienteId());
        consulta.setParameter("sinRegistro", persona.clienteSinRegistroId());
        consulta.setParameter("desde", desde);
        consulta.setParameter("hasta", hasta);
        Object r = consulta.getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    @Override
    @Transactional
    public void completarCliente(int personaId, Integer clienteId, Integer clienteSinRegistroId) {
        if (clienteId == null && clienteSinRegistroId == null) {
            return;
        }
        personas.findById(personaId).ifPresent(e -> {
            boolean cambio = false;
            if (e.getClienteId() == null && clienteId != null) {
                e.setClienteId(clienteId);
                cambio = true;
            }
            if (e.getClienteSinRegistroId() == null && clienteSinRegistroId != null) {
                e.setClienteSinRegistroId(clienteSinRegistroId);
                cambio = true;
            }
            if (cambio) {
                personas.save(e);
            }
        });
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
        var campanaEntidad = campanas.findById(campanaId);
        // Las personas con compras ligadas en esta campaña; lo que llevan se calcula igual que para la meta (pagado entre las fechas de la campaña).
        return pedidos.resumenPorPersona(campanaId).stream()
                .map(f -> {
                    PersonaCampana persona = personas.findById((Integer) f[0]).map(PersonasCampanaJpaAdapter::aDominio).orElseThrow();
                    BigDecimal llevaPagado = campanaEntidad
                            .map(c -> acumulado(new Campana(c.getId(), c.getNombre(), c.getFechaInicio(), c.getFechaFin(), c.isActiva(),
                                    c.getMetaMonto(), List.of()), persona))
                            .orElse((BigDecimal) f[1]);
                    return new ResumenDePersona(persona, llevaPagado, ((Number) f[2]).longValue());
                })
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(int personaId) {
        pedidos.borrarDePersona(personaId);
        personas.deleteById(personaId);
    }

    private static PersonaCampana aDominio(CampanaPersonaEntidad e) {
        return new PersonaCampana(e.getId(), e.getUuid(), e.getNombre(), e.getCorreo(), e.getTelefono(), e.isConsentimiento(),
                e.getClienteId(), e.getClienteSinRegistroId());
    }
}
