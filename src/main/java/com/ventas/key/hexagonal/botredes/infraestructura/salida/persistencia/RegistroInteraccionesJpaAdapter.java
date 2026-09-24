package com.ventas.key.hexagonal.botredes.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.ComentarioOriginal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RegistroInteraccionesPort;
import com.ventas.key.mis.productos.redessociales.ComentarioSocial;
import com.ventas.key.mis.productos.redessociales.IComentarioSocialRepository;
import com.ventas.key.mis.productos.redessociales.IMensajeDirectoSocialRepository;
import com.ventas.key.mis.productos.redessociales.MensajeDirectoSocial;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Comentarios en {@code comentario_social}, mensajes directos en {@code mensaje_directo_social}.
 * Los IDs de persona de Facebook e Instagram no se cruzan, por eso "primera vez" no filtra por red.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class RegistroInteraccionesJpaAdapter implements RegistroInteraccionesPort {

    private final IComentarioSocialRepository comentarios;
    private final IMensajeDirectoSocialRepository mensajes;

    public RegistroInteraccionesJpaAdapter(IComentarioSocialRepository comentarios,
                                           IMensajeDirectoSocialRepository mensajes) {
        this.comentarios = comentarios;
        this.mensajes = mensajes;
    }

    @Override
    public boolean yaProcesada(Canal canal, String id) {
        return canal == Canal.COMENTARIO
                ? comentarios.findByCommentId(id).isPresent()
                : mensajes.findByMid(id).isPresent();
    }

    @Override
    public boolean yaLeHabiamosEscrito(Canal canal, String autorId) {
        return canal == Canal.COMENTARIO ? comentarios.existsByAutorId(autorId) : mensajes.existsByAutorId(autorId);
    }

    @Override
    public void guardar(Interaccion i, String respuesta, String idRespuesta) {
        if (i.canal() == Canal.COMENTARIO) {
            ComentarioSocial fila = new ComentarioSocial();
            fila.setCommentId(i.id());
            fila.setPostId(i.publicacionId());
            fila.setRedSocial(i.red().codigo());
            fila.setAutorId(i.autorId());
            fila.setMensaje(i.texto());
            fila.setRespuesta(respuesta);
            fila.setRespuestaCommentId(idRespuesta);
            fila.setFecha(LocalDateTime.now());
            comentarios.save(fila);
            return;
        }
        MensajeDirectoSocial fila = new MensajeDirectoSocial();
        fila.setMid(i.id());
        fila.setRedSocial(i.red().codigo());
        fila.setAutorId(i.autorId());
        fila.setMensaje(i.tieneTexto() ? i.texto() : "(foto, audio o sticker)");
        fila.setRespuesta(respuesta);
        fila.setRespuestaMid(idRespuesta);
        fila.setFecha(LocalDateTime.now());
        mensajes.save(fila);
    }

    @Override
    public boolean esRespuestaDelBot(Canal canal, String id) {
        return canal == Canal.COMENTARIO
                ? comentarios.existsByRespuestaCommentId(id)
                : mensajes.existsByRespuestaMid(id);
    }

    @Override
    public Optional<ComentarioOriginal> comentarioOriginal(String commentId) {
        return comentarios.findByCommentId(commentId)
                .map(c -> new ComentarioOriginal(c.getAutorId(), c.getPostId()));
    }
}
