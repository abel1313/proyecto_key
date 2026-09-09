package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cinta_promocion")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CintaPromocion extends BaseId {

    @NotBlank(message = "El texto es requerido")
    @Size(max = 120, message = "El texto no puede superar 120 caracteres")
    @Column(nullable = false, length = 120)
    private String texto;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Integer orden = 0;
}
