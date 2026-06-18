package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_mensaje")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "sesion_id", nullable = false, length = 36)
    @JsonIgnore
    private String sesionId;

    @Column(name = "remitente", nullable = false, length = 10)
    private String remitente;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
