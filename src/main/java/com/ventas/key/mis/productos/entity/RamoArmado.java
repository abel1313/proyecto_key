package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Ramo preconfigurado ("armado"): el admin ya valido que esta cantidad de flores, de este color
// especifico, forma bien el circulo (cantidadFlorValida, a nivel especie) y ya aplico las
// reglas obligatorias (papel si supera el umbral) al precio.
@Entity
@Table(name = "ramo_armado")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RamoArmado extends BaseId {

    @Column(nullable = false, length = 150)
    private String nombre;

    // Foto de referencia de como va a quedar el ramo. Simple URL por ahora -- no pasa por
    // micro_imagenes, el admin sube la imagen por fuera y pega el link aqui.
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @ManyToOne
    @JoinColumn(name = "color_flor_id", nullable = false)
    private ColorFlor colorFlor;

    @ManyToOne
    @JoinColumn(name = "cantidad_flor_valida_id", nullable = false)
    private CantidadFlorValida cantidadFlorValida;

    @Column(name = "precio_flores", nullable = false)
    private Double precioFlores;

    @Column(name = "papel_incluido", nullable = false)
    private Boolean papelIncluido = false;

    @Column(name = "precio_papel")
    private Double precioPapel;

    // Congelado igual que precioFlores/precioPapel al momento de guardar -- ya esta sumado en
    // precioTotal, sin aparecer como accesorio (decision del dueno: un solo precio de ramo).
    @Column(name = "precio_mano_de_obra")
    private Double precioManoDeObra;

    @Column(name = "precio_total", nullable = false)
    private Double precioTotal;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "ramoArmado", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RamoArmadoAccesorio> accesorios;
}
