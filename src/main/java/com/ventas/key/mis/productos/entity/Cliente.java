package com.ventas.key.mis.productos.entity;


import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
  private String nombrePersona;
  
  private String segundoNombre;
  @NotEmpty(message = "El apeido paterno no deberia ir vacio")
  @NotNull( message = "El apeido paterno es requerido")
  private String apeidoPaterno;

  @NotEmpty(message = "El apeido materno no deberia ir vacio")
  @NotNull( message = "El apeido materno es requerido")
  
  private String apeidoMaterno;
  
  private Date fechaNacimiento;

  private String sexo;
  
  private String correoElectronico;
  
  private String numeroTelefonico;
}
