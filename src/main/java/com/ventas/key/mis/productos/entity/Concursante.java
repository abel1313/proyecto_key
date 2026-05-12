package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "concursantes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Concursante extends BaseId {

    @NotBlank(message = "El nombre es requerido")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "descartado", nullable = false)
    private boolean descartado = false;

    @Column(name = "orden_desde", nullable = false)
    private int ordenDesde = 1;

    @Column(name = "palabra_clave", length = 50)
    private String palabraClave;

    @Column(name = "cliente_pedido_id")
    private Integer clientePedidoId;

    @Column(name = "boletos_base", nullable = false)
    private int boletosBase = 1;

    @Column(name = "boletos", nullable = false)
    private int boletos = 1;

    @ManyToOne
    @JoinColumn(name = "configurar_rifa_id", nullable = false)
    @JsonIgnoreProperties("variantes")
    private ConfigurarRifa configurarRifa;
}