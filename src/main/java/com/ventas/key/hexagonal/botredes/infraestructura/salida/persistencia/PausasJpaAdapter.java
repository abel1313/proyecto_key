package com.ventas.key.hexagonal.botredes.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.PausasPort;
import com.ventas.key.mis.productos.redessociales.ComentarioPausa;
import com.ventas.key.mis.productos.redessociales.IComentarioPausaRepository;
import com.ventas.key.mis.productos.redessociales.IMensajePausaRepository;
import com.ventas.key.mis.productos.redessociales.MensajePausa;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * La columna {@code fecha} de la pausa es "desde cuándo": la pausa vence 30 minutos después, y
 * cada respuesta del admin la reinicia. No hizo falta migración: antes la fila era permanente.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class PausasJpaAdapter implements PausasPort {

    private final IComentarioPausaRepository comentarios;
    private final IMensajePausaRepository mensajes;

    public PausasJpaAdapter(IComentarioPausaRepository comentarios, IMensajePausaRepository mensajes) {
        this.comentarios = comentarios;
        this.mensajes = mensajes;
    }

    @Override
    public Optional<LocalDateTime> desde(Canal canal, String autorId, String publicacionId) {
        return canal == Canal.COMENTARIO
                ? comentarios.findFirstByAutorIdAndPostId(autorId, publicacionId).map(ComentarioPausa::getFecha)
                : mensajes.findByAutorId(autorId).map(MensajePausa::getFecha);
    }

    @Override
    public void pausar(Canal canal, String autorId, String publicacionId, LocalDateTime cuando) {
        if (canal == Canal.COMENTARIO) {
            ComentarioPausa pausa = comentarios.findFirstByAutorIdAndPostId(autorId, publicacionId)
                    .orElseGet(ComentarioPausa::new);
            pausa.setAutorId(autorId);
            pausa.setPostId(publicacionId);
            pausa.setFecha(cuando);
            comentarios.save(pausa);
            return;
        }
        MensajePausa pausa = mensajes.findByAutorId(autorId).orElseGet(MensajePausa::new);
        pausa.setAutorId(autorId);
        pausa.setFecha(cuando);
        mensajes.save(pausa);
    }
}
