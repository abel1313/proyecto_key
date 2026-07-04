package com.ventas.key.mis.productos.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Cliente extends BaseId{

      @NotNull( message = "El nombre es requerido")
      @NotEmpty(message = "El nombre no deberia ir vacio")
      @Column(name = "nombre_persona")
      private String nombrePersona;

        @Column(name = "segundo_nombre")
        private String segundoNombre;

      @NotEmpty(message = "El apeido paterno no deberia ir vacio")
      @NotNull( message = "El apeido paterno es requerido")
      @Column(name = "apeido_paterno")
      private String apeidoPaterno;

      // Opcional desde mejora 15 (PLAN_MEJORAS.md) — antes era obligatorio (@NotEmpty/@NotNull).
      @Column(name = "apeido_materno")
      private String apeidoMaterno;
      @Column(name = "fecha_nacimiento")
      private LocalDate fechaNacimiento;

      private String sexo;

      @NotBlank(message = "El correo electronico es requerido")
      @Email(message = "El correo electronico debe tener un formato valido")
      @Column(name = "correo_electronico")
      private String correoElectronico;

      @NotBlank(message = "El numero telefonico es requerido")
      @Pattern(regexp = "^\\d{10}$", message = "El numero telefonico debe tener 10 digitos")
      @Column(name = "numero_telefonico")
      private String numeroTelefonico;

      @Column(name = "correo_verificado")
      private Boolean correoVerificado = Boolean.FALSE;

      @Column(name = "codigo_verificacion")
      private String codigoVerificacion;

      @Column(name = "codigo_verificacion_expira")
      private LocalDateTime codigoVerificacionExpira;

      // Correo nuevo escrito por el cliente, esperando verificacion (mejora 15). correoElectronico
      // NO cambia hasta que se verifique este valor con el codigo enviado aqui.
      @Column(name = "correo_pendiente")
      private String correoPendiente;

      // true = nombre/apeidoPaterno/numeroTelefonico/correoElectronico ya estan llenos (mejora 15).
      // Se recalcula solo en cada guardado, ver recalcularDatosCompletos().
      @Column(name = "datos_completos")
      private Boolean datosCompletos = Boolean.FALSE;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    @JsonBackReference
    private Usuario usuario;

      @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
      @JsonManagedReference
      private Set<Direccion> listDirecciones;

      @PrePersist
      @PreUpdate
      private void recalcularDatosCompletos() {
            this.datosCompletos = nombrePersona != null && !nombrePersona.isBlank()
                  && apeidoPaterno != null && !apeidoPaterno.isBlank()
                  && numeroTelefonico != null && !numeroTelefonico.isBlank()
                  && correoElectronico != null && !correoElectronico.isBlank();
      }

      public Cliente(
            String nombrePersona,
            String segundoNombre,
            String apeidoPaterno,
            String apeidoMaterno,
            LocalDate fechaNacimiento,
            String sexo,
            String correoElectronico,
            String numeroTelefonico,
            Set<Direccion> listDirecciones){

            this.nombrePersona = nombrePersona;
            this.segundoNombre = segundoNombre;
            this.apeidoPaterno = apeidoPaterno;
            this.apeidoMaterno = apeidoMaterno;
            this.fechaNacimiento = fechaNacimiento;
            this.sexo = sexo;
            this.correoElectronico = correoElectronico;
            this.numeroTelefonico = numeroTelefonico;
            this.listDirecciones = listDirecciones;

      }


}
