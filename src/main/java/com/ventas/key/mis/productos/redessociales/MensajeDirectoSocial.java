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

// Registro de cada mensaje directo (DM) que el bot proceso -- mismo proposito que
// ComentarioSocial pero para mensajeria en vez de comentarios publicos:
// 1) Idempotencia: Meta puede reenviar el mismo evento de webhook mas de una vez; con mid UNIQUE
//    no se responde duplicado.
// 2) Saber si es la "primera vez" que ese autor_id escribe (saludo de bienvenida obligatorio).
// Ver InstagramDirectMessageBotService.
@Entity
@Table(name = "mensaje_directo_social")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MensajeDirectoSocial extends BaseId {

    @Column(name = "mid", unique = true, length = 100)
    private String mid;

    @Column(name = "red_social", length = 20)
    private String redSocial;

    @Column(name = "autor_id", length = 100)
    private String autorId;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    // El mid que devolvio Instagram de NUESTRA propia respuesta -- sirve para reconocer, cuando
    // ese mismo mid vuelve por el webhook como "echo" (Meta notifica los mensajes que manda la
    // propia pagina/cuenta tambien), que es un eco de nuestro propio bot y no una respuesta manual
    // del admin. Ver InstagramDirectMessageBotService.detectarRespuestaManualYPausar.
    @Column(name = "respuesta_mid", length = 100)
    private String respuestaMid;

    @Column(name = "fecha")
    private LocalDateTime fecha;
}
