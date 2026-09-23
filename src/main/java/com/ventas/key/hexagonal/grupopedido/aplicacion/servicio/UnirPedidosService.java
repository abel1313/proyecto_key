package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.CobroDelGrupoInvalidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoPedidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.SeparacionInvalidaException;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoRegistrado;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Movimiento;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PlanDeSeparacion;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.ReacomodoDeAbonos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Separacion;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Reparto;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.UnionDePedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonoPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonosDelGrupoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.EstadoDePagoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.BitacoraPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.ConfirmarPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orquesta unir, abonar y deshacer grupos de pedidos.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p><b>Todo o nada.</b> Un abono al grupo son varios abonos a pedidos; si el tercero falla, los
 * dos primeros no pueden quedar hechos. Por eso cada operacion es {@code @Transactional}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UnirPedidosService implements UnirPedidosCasoUso {

    private final PedidosDelGrupoPort pedidos;
    private final GrupoPedidosPort grupos;
    private final AbonoPedidoPort abonos;
    private final BitacoraPedidoPort bitacora;
    private final ConfirmarPedidoPort confirmar;
    private final AbonosDelGrupoPort abonosDelGrupo;
    private final EstadoDePagoPort estadoDePago;

    @Override
    @Transactional
    public GrupoPedidos unir(List<Integer> pedidoIds, Integer pedidoTitularId, String nota, Integer usuarioId) {
        List<Integer> distintos = pedidoIds == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(pedidoIds.stream().filter(java.util.Objects::nonNull).toList()));

        UnionDePedidos.validar(distintos, pedidos.buscar(distintos), pedidoTitularId,
                grupos.gruposActivosDe(distintos));

        Integer grupoId = grupos.crear(pedidoTitularId, distintos, limpiar(nota), usuarioId);

        String lista = distintos.stream().map(id -> "#" + id).collect(Collectors.joining(", "));
        for (Integer pedidoId : distintos) {
            bitacora.anotar(pedidoId, String.format("[%s] Se unio al grupo #%d con los pedidos %s (titular: pedido #%d)%s",
                    LocalDate.now(), grupoId, lista, pedidoTitularId, sufijo(nota)));
        }
        log.info("Grupo {} creado con los pedidos {} (titular {})", grupoId, distintos, pedidoTitularId);
        return consultar(grupoId);
    }

    @Override
    @Transactional(readOnly = true)
    public GrupoPedidos consultar(Integer grupoId) {
        RegistroGrupo registro = grupos.buscar(grupoId).orElseThrow(() -> new GrupoNoEncontradoException(grupoId));
        return armar(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GrupoPedidos> grupoActivoDe(Integer pedidoId) {
        return grupos.grupoActivoDe(pedidoId).flatMap(grupos::buscar).map(this::armar);
    }

    @Override
    @Transactional
    public ResultadoAbono abonar(Integer grupoId, AbonoAlGrupo abono, Integer usuarioId) {
        GrupoPedidos grupo = consultar(grupoId);
        long cambio = abono.cambioCentavos();
        List<Reparto> repartos = grupo.repartir(abono.montoCentavos());

        String nota = "Abono del grupo #" + grupoId + sufijo(abono.nota());
        for (Reparto reparto : repartos) {
            abonos.abonar(reparto.pedidoId(), reparto.montoCentavos(), abono.metodo(), nota, usuarioId);
        }
        log.info("Grupo {}: abono de {} centavos repartido en {}", grupoId, abono.montoCentavos(), repartos);
        return new ResultadoAbono(consultar(grupoId), repartos, cambio);
    }

    @Override
    @Transactional
    public ResultadoCobro cobrarDeContado(Integer grupoId, Integer pagosYMesesId, Integer usuarioId) {
        if (pagosYMesesId == null) {
            throw new CobroDelGrupoInvalidoException("Falta elegir la forma de pago");
        }
        List<Integer> porCobrar = consultar(grupoId).porCobrarDeContado();
        for (Integer pedidoId : porCobrar) {
            confirmar.confirmarDeContado(pedidoId, pagosYMesesId);
            bitacora.anotar(pedidoId, String.format("[%s] Cobrado de contado junto con el grupo #%d",
                    LocalDate.now(), grupoId));
        }
        log.info("Grupo {}: cobrados de contado los pedidos {}", grupoId, porCobrar);
        return new ResultadoCobro(consultar(grupoId), porCobrar);
    }

    @Override
    @Transactional
    public ResultadoSeparacion separar(Integer grupoId, List<Integer> salen, Map<Integer, Long> reparto,
                                       Integer nuevoTitular, String motivo, Integer usuarioId) {
        GrupoPedidos grupo = consultar(grupoId);
        List<Integer> vivos = grupo.pedidos().stream().filter(p -> !p.estaCancelado())
                .map(PedidoDelGrupo::pedidoId).toList();
        List<AbonoRegistrado> abonos = grupo.esDeCredito() ? abonosDelGrupo.abonosDe(vivos) : List.of();

        PlanDeSeparacion plan = Separacion.planear(grupo, abonos, salen, reparto, nuevoTitular);

        if (!plan.objetivos().isEmpty()) {
            String nota = "Reparto al separar el grupo #" + grupoId;
            for (Movimiento m : ReacomodoDeAbonos.planear(abonos, plan.objetivos())) {
                abonosDelGrupo.mover(m, nota);
            }
            for (Integer pedidoId : plan.objetivos().keySet()) {
                estadoDePago.ajustarAlosAbonos(pedidoId, usuarioId);
            }
        }

        grupos.marcarDeshecho(grupoId, usuarioId);
        Integer grupoNuevo = null;
        if (!plan.terminaElGrupo()) {
            grupoNuevo = grupos.crear(plan.titular(), plan.quedan(),
                    limpiar("Sigue del grupo #" + grupoId + sufijo(motivo)), usuarioId);
        }

        GrupoPedidos despues = consultar(grupoId);
        String quedanTexto = plan.quedan().stream().map(id -> "#" + id).collect(Collectors.joining(", "));
        for (PedidoDelGrupo p : despues.pedidos()) {
            String texto;
            if (plan.salen().contains(p.pedidoId())) {
                texto = String.format("[%s] Se separo del grupo #%d%s%s", LocalDate.now(), grupoId,
                        plan.objetivos().containsKey(p.pedidoId())
                                ? String.format(": se quedo con $%.2f de lo abonado y debe $%.2f",
                                        p.cobradoCentavos() / 100.0, p.saldoCentavos() / 100.0)
                                : "",
                        sufijo(motivo));
            } else {
                texto = String.format("[%s] Salieron pedidos del grupo #%d; sigue unido en el grupo #%d con %s (titular: pedido #%d)%s",
                        LocalDate.now(), grupoId, grupoNuevo, quedanTexto, plan.titular(), sufijo(motivo));
            }
            bitacora.anotar(p.pedidoId(), texto);
        }
        log.info("Grupo {} separado: salen {}, quedan {} en el grupo {}, reparto {}",
                grupoId, plan.salen(), plan.quedan(), grupoNuevo, plan.objetivos());
        return new ResultadoSeparacion(despues, grupoNuevo);
    }

    @Override
    @Transactional
    public GrupoPedidos cambiarTitular(Integer grupoId, Integer pedidoTitularId, Integer usuarioId) {
        GrupoPedidos grupo = consultar(grupoId);
        if (!grupo.activo()) {
            throw new GrupoPedidoException("El grupo " + grupoId + " ya se habia separado");
        }
        boolean esMiembro = grupo.pedidos().stream()
                .anyMatch(p -> p.pedidoId().equals(pedidoTitularId) && !p.estaCancelado());
        if (!esMiembro) {
            throw new GrupoPedidoException("Quien recoge tiene que ser uno de los pedidos del grupo");
        }
        if (pedidoTitularId.equals(grupo.pedidoTitularId())) {
            return grupo;
        }
        grupos.cambiarTitular(grupoId, pedidoTitularId);
        for (PedidoDelGrupo p : grupo.pedidos()) {
            bitacora.anotar(p.pedidoId(), String.format("[%s] En el grupo #%d ahora paga y recoge el cliente del pedido #%d (antes #%d)",
                    LocalDate.now(), grupoId, pedidoTitularId, grupo.pedidoTitularId()));
        }
        return consultar(grupoId);
    }

    @Override
    @Transactional
    public GrupoPedidos deshacer(Integer grupoId, String motivo, Integer usuarioId) {
        GrupoPedidos grupo = consultar(grupoId);
        if (!grupo.activo()) {
            throw new GrupoPedidoException("El grupo " + grupoId + " ya se habia deshecho");
        }
        if (grupo.esDeCredito() && grupo.pagadoCentavos() > 0) {
            throw new SeparacionInvalidaException("El cliente ya dio dinero en este grupo: al separar hay que decir "
                    + "cuanto se queda cada pedido");
        }
        grupos.marcarDeshecho(grupoId, usuarioId);
        for (PedidoDelGrupo pedido : grupo.pedidos()) {
            bitacora.anotar(pedido.pedidoId(), String.format("[%s] Se deshizo el grupo #%d; el pedido conserva sus articulos y abonos%s",
                    LocalDate.now(), grupoId, sufijo(motivo)));
        }
        log.info("Grupo {} deshecho", grupoId);
        return consultar(grupoId);
    }

    private GrupoPedidos armar(RegistroGrupo registro) {
        return GrupoPedidos.de(registro, pedidos.buscar(registro.pedidoIds()));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String sufijo(String texto) {
        String limpio = limpiar(texto);
        return limpio == null ? "" : ": " + limpio;
    }
}
