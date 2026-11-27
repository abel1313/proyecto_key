package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LugarDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.LugaresDeEntregaPort;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** El catálogo de zonas ({@code lugares_entrega}), el mismo del que salen los lugares que menciona el bot. [Hexagonal: Driven Adapter] */
@Component
public class LugaresDeEntregaJpaAdapter implements LugaresDeEntregaPort {

    private final ILugarEntregaRepository lugares;

    public LugaresDeEntregaJpaAdapter(ILugarEntregaRepository lugares) {
        this.lugares = lugares;
    }

    @Override
    public List<LugarDeEntrega> todos() {
        return lugares.findAll().stream()
                .filter(l -> l.getNombre() != null && !l.getNombre().isBlank())
                .map(l -> new LugarDeEntrega(l.getId(), l.getNombre(), Boolean.TRUE.equals(l.getEsRecogerEnTienda())))
                .toList();
    }
}
