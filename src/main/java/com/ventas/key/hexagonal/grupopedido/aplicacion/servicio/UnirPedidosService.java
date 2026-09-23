package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoPedidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Reparto;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.UnionDePedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonoPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.BitacoraPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
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
    public GrupoPedidos deshacer(Integer grupoId, String motivo, Integer usuarioId) {
        GrupoPedidos grupo = consultar(grupoId);
        if (!grupo.activo()) {
            throw new GrupoPedidoException("El grupo " + grupoId + " ya se habia deshecho");
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
        List<PedidoDelGrupo> encontrados = pedidos.buscar(registro.pedidoIds());
        return new GrupoPedidos(registro.grupoId(), registro.pedidoTitularId(), registro.activo(),
                registro.creado(), registro.nota(), encontrados);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String sufijo(String texto) {
        String limpio = limpiar(texto);
        return limpio == null ? "" : ": " + limpio;
    }
}
