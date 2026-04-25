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
import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_usuario")
    private Roles roles;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_permiso",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permisosExtra = new HashSet<>();

    @Column(nullable = false)
    private Boolean enabled = true;

    @OneToOne(mappedBy = "usuario")
    @JsonManagedReference
    private Cliente cliente;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(roles.getNombreRol()));
        roles.getPermisos().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getNombrePermiso())));
        permisosExtra.forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getNombrePermiso())));
        return authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return "Usuario{username='" + username + "', email='" + email + "'}";
    }
}