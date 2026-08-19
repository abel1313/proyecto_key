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

// Marca que un admin ya contestó manualmente (desde la app real de Facebook, no desde nuestro
// bot) a un cliente en un post especifico -- a partir de ahi, el bot deja de auto-contestarle a
// ESE cliente en ESE post (indefinido, hasta que un admin la borre a mano si hiciera falta). Otros
// posts o otros clientes siguen funcionando normal. Ver
// FacebookCommentBotService.detectarRespuestaManualYPausar.
@Entity
@Table(name = "comentario_pausa")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioPausa extends BaseId {

    @Column(name = "autor_id", length = 100)
    private String autorId;

    @Column(name = "post_id", length = 100)
    private String postId;

    @Column(name = "fecha")
    private LocalDateTime fecha;
}
