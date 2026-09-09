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

// Un set fijo de hashtags por red (facebook/instagram/tiktok) que el front precarga siempre al
// abrir el formulario de publicar, para que el admin no los vuelva a escribir a mano cada vez.
// Editable por el admin -- no hay historial, solo el valor vigente por red.
@Entity
@Table(name = "hashtags_default")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HashtagsDefault extends BaseId {

    @Column(name = "red_social", unique = true, length = 20)
    private String redSocial;

    @Column(columnDefinition = "TEXT")
    private String hashtags;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
