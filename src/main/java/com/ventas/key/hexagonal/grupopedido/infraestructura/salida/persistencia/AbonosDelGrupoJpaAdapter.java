package com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoRegistrado;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Movimiento;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.AbonosDelGrupoPort;
import com.ventas.key.mis.productos.entity.AbonoPedido;
import com.ventas.key.mis.productos.repository.IAbonoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Lee y mueve filas de {@code abono_pedido}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Mover cambia el {@code pedido_id} de la fila y conserva fecha y forma de pago, asi el corte
 * de caja de ese dia sigue sumando lo mismo. Cuando solo se mueve una parte, la fila se parte en
 * dos.
 */
@Component
@RequiredArgsConstructor
public class AbonosDelGrupoJpaAdapter implements AbonosDelGrupoPort {

    private final IAbonoRepository abonoRepository;
    private final IPedidoRepository pedidoRepository;

    @Override
    public List<AbonoRegistrado> abonosDe(Collection<Integer> pedidoIds) {
        if (pedidoIds == null || pedidoIds.isEmpty()) {
            return List.of();
        }
        return abonoRepository.findByPedidoIdIn(pedidoIds).stream()
                .map(a -> new AbonoRegistrado(a.getId(), a.getPedido().getId(),
                        Math.round(a.getMonto() * 100), a.getFechaPago()))
                .toList();
    }

    @Override
    public void mover(Movimiento m, String nota) {
        AbonoPedido abono = abonoRepository.findById(m.abonoId())
                .orElseThrow(() -> new IllegalStateException("Abono no encontrado: " + m.abonoId()));
        long centavosDelAbono = Math.round(abono.getMonto() * 100);
        String marca = String.format("%s: del pedido #%d al #%d", nota, m.dePedidoId(), m.aPedidoId());

        if (m.centavos() >= centavosDelAbono) {
            abono.setPedido(pedidoRepository.getReferenceById(m.aPedidoId()));
            abono.setNota(conMarca(abono.getNota(), marca));
            abonoRepository.save(abono);
            return;
        }

        abono.setMonto((centavosDelAbono - m.centavos()) / 100.0);
        abonoRepository.save(abono);

        AbonoPedido parte = new AbonoPedido();
        parte.setPedido(pedidoRepository.getReferenceById(m.aPedidoId()));
        parte.setMonto(m.centavos() / 100.0);
        parte.setFechaPago(abono.getFechaPago());
        parte.setMetodoPago(abono.getMetodoPago());
        parte.setNota(conMarca(abono.getNota(), marca));
        abonoRepository.save(parte);
    }

    /** abono_pedido.nota es VARCHAR(200): la marca del reparto va primero para que no se corte. */
    private static String conMarca(String notaPrevia, String marca) {
        String texto = notaPrevia == null || notaPrevia.isBlank() ? marca : marca + " | " + notaPrevia;
        return texto.length() > 200 ? texto.substring(0, 200) : texto;
    }
}
