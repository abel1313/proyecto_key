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

// Marca que un admin ya contesto manualmente (desde la app real de Instagram, no desde el bot) a
// un cliente por mensaje directo -- a partir de ahi el bot deja de auto-contestarle a ESE cliente
// (indefinido, hasta que se borre a mano). A diferencia de ComentarioPausa no hay post_id -- un DM
// no esta ligado a una publicacion, la pausa es por conversacion completa con ese autor.
// Ver InstagramDirectMessageBotService.detectarRespuestaManualYPausar.
@Entity
@Table(name = "mensaje_directo_pausa")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MensajePausa extends BaseId {

    @Column(name = "autor_id", unique = true, length = 100)
    private String autorId;

    @Column(name = "fecha")
    private LocalDateTime fecha;
}
