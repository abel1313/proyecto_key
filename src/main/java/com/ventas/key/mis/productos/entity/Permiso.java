package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permisos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Permiso extends BaseId {

    @Column(name = "nombre_permiso", nullable = false, unique = true, length = 60)
    private String nombrePermiso;

    @JsonIgnore
    @ManyToMany(mappedBy = "permisos")
    private Set<Roles> roles = new HashSet<>();
}