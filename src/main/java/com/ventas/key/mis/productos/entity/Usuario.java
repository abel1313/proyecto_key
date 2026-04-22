package com.ventas.key.mis.productos.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "usuario_modificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String email;



//    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(
//            name = "usuarios_roles", // tabla intermedia
//            joinColumns = @JoinColumn(name = "usuario_id"), // FK hacia usuarios
//            inverseJoinColumns = @JoinColumn(name = "rol_id") // FK hacia roles
//    )
//    private Set<Roles> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "rol_usuario")
    private Roles roles;

    @Column(nullable = false)
    private Boolean enabled = true;

    @OneToOne(mappedBy = "usuario")
    @JsonManagedReference
    private Cliente cliente;

    // Métodos requeridos por UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(roles.getNombreRol()));
    }


    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }


    @Override
    public String toString() {
        return "Usuario{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
