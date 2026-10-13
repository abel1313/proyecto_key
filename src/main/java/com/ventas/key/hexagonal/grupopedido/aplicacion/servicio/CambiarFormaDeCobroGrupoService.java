package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.CambiarFormaDeCobroGrupoCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.BitacoraPedidoPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.FormaDeCobroPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cambia la forma de cobro del grupo completo (R20).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p>El dominio decide si se puede y cuales cambian; aqui solo se orquesta: cada pedido cambia por
 * el mismo camino que un pedido suelto, y el dinero de ese momento entra como abono del grupo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CambiarFormaDeCobroGrupoService implements CambiarFormaDeCobroGrupoCasoUso {

    private final UnirPedidosCasoUso grupos;
    private final FormaDeCobroPort formaDeCobro;
    private final BitacoraPedidoPort bitacora;

    @Override
    @Transactional
    public Resultado cambiar(Integer grupoId, String tipoNuevo, AbonoAlGrupo cobro, String nota, Integer usuarioId) {
        GrupoPedidos grupo = grupos.consultar(grupoId);
        String tipoAnterior = grupo.tipo();
        long cobroAhora = cobro == null ? 0 : cobro.montoCentavos();
        // Antes de tocar nada: si el dominio dice que no, no cambia ninguno.
        List<Integer> cambian = grupo.porCambiarFormaDeCobro(tipoNuevo, cobroAhora);
        String tipo = tipoNuevo.trim().toUpperCase();

        String lista = grupo.pedidos().stream().filter(p -> !p.estaCancelado())
                .map(p -> "#" + p.pedidoId()).collect(Collectors.joining(", "));
        String antes = comoSeVe(grupo, tipoAnterior);
        String extra = nota == null || nota.isBlank() ? "" : ": " + nota.trim();
        for (Integer pedidoId : cambian) {
            formaDeCobro.cambiar(pedidoId, tipo, nota == null || nota.isBlank() ? null : nota.trim());
            bitacora.anotar(pedidoId, String.format("[%s] Forma de cobro del grupo #%d: %s → %s (pedidos %s)%s",
                    LocalDate.now(), grupoId, antes, GrupoPedidos.nombre(tipo), lista, extra));
        }
        // Lo que deja el cliente en este momento: igual que "Abonar al grupo" (R6).
        UnirPedidosCasoUso.ResultadoAbono abono = cobroAhora > 0 ? grupos.abonar(grupoId, cobro, usuarioId) : null;

        log.info("Grupo {}: forma de cobro {} -> {} en los pedidos {} (abono {} centavos)",
                grupoId, tipoAnterior, tipo, cambian, cobroAhora);
        return new Resultado(grupos.consultar(grupoId), tipoAnterior, cambian, abono);
    }

    /** "Pendiente" para lo que el cliente pidio desde su cuenta y nadie ha cobrado. */
    private static String comoSeVe(GrupoPedidos grupo, String tipo) {
        boolean pendiente = "NORMAL".equals(tipo) && grupo.pedidos().stream()
                .filter(p -> !p.estaCancelado()).allMatch(p -> "Pendiente".equalsIgnoreCase(p.estado()));
        return pendiente ? "Pendiente" : GrupoPedidos.nombre(tipo);
    }
}
