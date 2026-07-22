package com.ventas.key.mis.productos.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "clientes_sin_registro")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClienteSinRegistro extends BaseId{

        @NotNull( message = "El nombre es requerido")
        @NotEmpty(message = "El nombre no deberia ir vacio")
        @Column(name = "nombre_persona")
        private String nombrePersona;

        @Column(name = "segundo_nombre")
        private String segundoNombre;

        @Column(name = "apeido_paterno")
        private String apeidoPaterno;

        @Column(name = "apeido_materno")
        private String apeidoMaterno;

        @Column(name = "fecha_nacimiento")
        private LocalDate fechaNacimiento;

        private String sexo;

        @Column(name = "correo_electronico")
        private String correoElectronico;

        @Column(name = "numero_telefonico")
        private String numeroTelefonico;

        @Column(name = "correo_verificado")
        private Boolean correoVerificado = Boolean.FALSE;

        @Column(name = "codigo_verificacion")
        private String codigoVerificacion;

        @Column(name = "codigo_verificacion_expira")
        private LocalDateTime codigoVerificacionExpira;

        @Column(name = "creado_en")
        private LocalDateTime creadoEn;

        @PrePersist
        private void marcarCreacion() {
                if (this.creadoEn == null) {
                        this.creadoEn = LocalDateTime.now();
                }
        }




}
