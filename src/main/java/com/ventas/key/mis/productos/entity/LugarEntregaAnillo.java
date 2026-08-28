package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.*;
import lombok.*;

// Anillo (rango de distancia) de cobro dentro de una zona -- ver DISENO_ZONAS_POR_ANILLO.md en
// el repo compartido. Un LugarEntrega puede tener 0 anillos (se comporta como hoy: costoEnvio
// fijo) o varios; el punto marcado en el mapa se cobra segun en que anillo caiga la distancia
// al centro de la zona (LugarEntrega.latitud/longitud).
@Entity
@Table(name = "lugar_entrega_anillo")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class LugarEntregaAnillo extends BaseId {

    @ManyToOne
    @JoinColumn(name = "lugar_entrega_id", nullable = false)
    private LugarEntrega lugarEntrega;

    // Radio del circulo, en metros, medido desde el centro de la zona (LugarEntrega.latitud/longitud).
    @Column(name = "radio_metros", nullable = false)
    private Double radioMetros;

    @Column(name = "costo_envio", nullable = false)
    private Double costoEnvio;

    // Desempate si dos radios quedan casi iguales por error de captura -- en la practica se
    // resuelve por radioMetros ascendente, este campo es solo un respaldo manual.
    @Column(name = "orden")
    private Integer orden;

    // Variante "sombra" -- mismo patron que LugarEntrega.variante (ver ProductoSombraServiceImpl).
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
