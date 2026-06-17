package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sesion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sesion_id", nullable = false, unique = true, length = 36)
    private String sesionId;

    @Column(name = "identificador", nullable = false, length = 100)
    private String identificador;

    @Column(name = "nombre_usuario", length = 100)
    private String nombreUsuario;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;
}
