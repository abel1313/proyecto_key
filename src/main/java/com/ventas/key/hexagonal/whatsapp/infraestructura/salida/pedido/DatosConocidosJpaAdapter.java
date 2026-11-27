package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DatosConocidos;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.DatosConocidosPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lo que dio el cliente en su último pedido hecho por el bot DESDE ese mismo número de WhatsApp (se reconoce por las
 * observaciones «Pedido hecho por WhatsApp +número»). Ya NO se busca por el teléfono que alguien escribió en su pedido:
 * así el correo de A nunca se le propone a V (hallazgo I7). Solo lectura.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class DatosConocidosJpaAdapter implements DatosConocidosPort {

    private final JdbcTemplate jdbc;

    public DatosConocidosJpaAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<DatosConocidos> porNumeroDeWhatsapp(String numeroDeWhatsapp) {
        if (numeroDeWhatsapp == null || !numeroDeWhatsapp.matches("\\d{6,20}")) {
            return Optional.empty();
        }
        String base = PedidosApartadoVentaAdapter.PREFIJO_OBSERVACIONES + numeroDeWhatsapp;
        List<DatosConocidos> l = jdbc.query("""
                SELECT c.nombre_persona, c.correo_electronico, p.direccion_entrega
                FROM pedidos p JOIN clientes_sin_registro c ON c.id = p.cliente_sin_registro_id
                WHERE (p.observaciones = ? OR p.observaciones LIKE ?)
                ORDER BY p.id DESC LIMIT 1
                """, (rs, n) -> new DatosConocidos(rs.getString(1), rs.getString(2), rs.getString(3)),
                base, base + " |%");
        return l.stream()
                .filter(c -> c.nombre() != null && !c.nombre().isBlank() && c.correo() != null && !c.correo().isBlank())
                .findFirst();
    }
}
