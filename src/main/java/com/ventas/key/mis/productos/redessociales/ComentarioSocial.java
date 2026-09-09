package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.entity.BaseId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Registro de cada comentario que el bot proceso en redes sociales -- sirve para 2 cosas:
// 1) Idempotencia: Meta a veces reenvia el mismo evento de webhook mas de una vez; si ya existe
//    una fila con este commentId, no se vuelve a procesar (evita responder duplicado).
// 2) Saber si es la "primera vez" que ese autor comenta -- si nunca hay una fila previa con su
//    autorId, el bot SIEMPRE contesta con un saludo (aunque no entienda la pregunta); de ahi en
//    adelante, si no entiende, se queda callado. Ver FacebookCommentBotService.
@Entity
@Table(name = "comentario_social")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioSocial extends BaseId {

    @Column(name = "comment_id", unique = true, length = 100)
    private String commentId;

    @Column(name = "post_id", length = 100)
    private String postId;

    @Column(name = "red_social", length = 20)
    private String redSocial;

    @Column(name = "autor_id", length = 100)
    private String autorId;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    // El comment_id que devolvió Facebook de NUESTRA propia respuesta -- sirve para reconocer,
    // cuando ese mismo id vuelve por el webhook (Meta notifica cualquier comentario nuevo, incluidos
    // los que hacemos nosotros), que es un eco de nuestro propio bot y no una respuesta manual del
    // admin. Ver FacebookCommentBotService.detectarRespuestaManualYPausar.
    @Column(name = "respuesta_comment_id", length = 100)
    private String respuestaCommentId;

    @Column(name = "fecha")
    private LocalDateTime fecha;
}
