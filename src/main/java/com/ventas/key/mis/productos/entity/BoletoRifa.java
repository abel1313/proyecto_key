package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Un boleto por cada acción distinta que un concursante hace en redes sociales
// (seguir, compartir, etc.) -- el nombre/datos del participante se registran una
// sola vez en Concursante; aquí solo se agrega la evidencia de la acción puntual.
@Entity
@Table(name = "boletos_rifa")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BoletoRifa extends BaseId {

    @ManyToOne
    @JoinColumn(name = "concursante_id", nullable = false)
    @JsonIgnoreProperties({"configurarRifa"})
    private Concursante concursante;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "url_perfil_red_social", length = 500)
    private String urlPerfilRedSocial;

    @Column(name = "url_seguimiento", length = 500)
    private String urlSeguimiento;

    // Varias publicaciones pueden servir de evidencia para un mismo boleto
    @ElementCollection
    @CollectionTable(name = "boleto_rifa_url_compartido", joinColumns = @JoinColumn(name = "boleto_rifa_id"))
    @Column(name = "url", length = 500)
    private List<String> urlsCompartido = new ArrayList<>();
}
