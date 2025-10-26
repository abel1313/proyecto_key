package com.ventas.key.mis.productos.entity;


import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

      @NotEmpty(message = "El apeido materno no deberia ir vacio")
      @NotNull( message = "El apeido materno es requerido")
      @Column(name = "apeido_materno")
      private String apeidoMaterno;
      @Column(name = "fecha_nacimiento")
      private LocalDate fechaNacimiento;

      private String sexo;

      @Column(name = "correo_electronico")
      private String correoElectronico;

      @Column(name = "numero_telefonico")
      private String numeroTelefonico;

      @OneToMany(mappedBy = "cliente", cascade = CascadeType.PERSIST, orphanRemoval = true)
      @JsonManagedReference
      private Set<Direccion> listDirecciones;


}
