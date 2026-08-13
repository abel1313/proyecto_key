package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lugares_entrega")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class LugarEntrega extends BaseId {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    // Costo de envio a ese lugar. NULL = no se maneja costo de envio configurado para este
    // lugar (ej. se sigue usando como catalogo generico de "quien recibe" sin cobro asociado).
    @Column(name = "costo_envio")
    private Double costoEnvio;

    // Variante "sombra" -- ver comentario en TipoFlor.variante. Solo se crea/sincroniza cuando
    // costoEnvio no es null (sin costo, no hace falta linea de cobro por envio).
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
