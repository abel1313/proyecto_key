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

    // Horas extra que se suman al minimo de anticipacion requerido por el tamano del ramo (ver
    // CantidadFlorValida.horasMinimasAnticipacion) por estar esta zona mas lejos/ser mas
    // complicado llegar. NULL = 0, no suma nada (mismo comportamiento que hoy).
    @Column(name = "horas_extra_anticipacion")
    private Integer horasExtraAnticipacion;

    // Centroide de la zona, para que el picker de mapa del front arranque centrado ahi en vez
    // de un punto fijo generico. NULL = zona vieja sin configurar, el front sigue usando su
    // centro por defecto.
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Variante "sombra" -- ver comentario en TipoFlor.variante. Solo se crea/sincroniza cuando
    // costoEnvio no es null (sin costo, no hace falta linea de cobro por envio).
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;

    // 2026-09-04: distingue la fila de este catalogo que representa "recoger en el local" de las
    // zonas de entrega reales (Tejupilco, Zacazonapan, etc.). El checkout normal de la tienda usa
    // esto para decidir si muestra el calendario de fecha de recogida (PedidoServiceImpl.savePedido
    // solo valida/rellena Pedido.fechaRecogida cuando el lugar elegido tiene esto en true) -- para
    // una zona de entrega real, la fecha la coordina el admin despues a mano (editar entrega), no
    // el cliente en el checkout. Debe haber como mucho una fila en true (no se valida en BD, es
    // responsabilidad de quien administra el catalogo).
    @Column(name = "es_recoger_en_tienda")
    private Boolean esRecogerEnTienda;

    // 2026-09-04: dia de la semana en que el dueno hace el viaje de entrega a esta zona (recurrente
    // -- se configura una vez, aplica cada semana), 1=lunes .. 7=domingo (java.time.DayOfWeek).
    // NULL = zona sin dia fijo configurado todavia (o "recoger en tienda", que no aplica). Usado
    // por EntregaZonaServiceImpl para sugerir la fecha al programar la entrega de la semana.
    @Column(name = "dia_entrega_semanal")
    private Integer diaEntregaSemanal;
}
