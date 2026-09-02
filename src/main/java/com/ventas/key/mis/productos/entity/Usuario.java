package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
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

    // @BatchSize: sin esto, cada fila de un listado paginado (getAllPage) dispara su propio
    // SELECT aparte para traer permisosExtra (N+1 -- 10 filas = 10 queries extra). Con
    // @BatchSize, Hibernate agrupa esas cargas en un solo SELECT ... WHERE usuario_id IN (...)
    // por página, sin afectar la paginación (a diferencia de un JOIN FETCH sobre una colección,
    // que Hibernate pagina en memoria -- por eso esto y no eso).
    @BatchSize(size = 25)
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

    @JsonIgnore
    @Column(name = "codigo_reset_password")
    private String codigoResetPassword;

    @JsonIgnore
    @Column(name = "codigo_reset_password_expira")
    private LocalDateTime codigoResetPasswordExpira;

    /**
     * Intentos fallidos consumidos contra el codigo de reset vigente. Al llegar a
     * {@code MAX_INTENTOS_CODIGO} el codigo se invalida y hay que pedir uno nuevo — sin esto,
     * el codigo de 6 digitos se puede probar por fuerza bruta durante sus 15 minutos de vida.
     */
    @JsonIgnore
    @Column(name = "intentos_codigo_reset")
    private Integer intentosCodigoReset = 0;

    @Column(name = "correo_verificado")
    private Boolean correoVerificado = Boolean.FALSE;

    @JsonIgnore
    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;

    @JsonIgnore
    @Column(name = "codigo_verificacion_expira")
    private LocalDateTime codigoVerificacionExpira;

    /**
     * Mismo mecanismo que {@link #intentosCodigoReset}, pero para el codigo de verificacion de
     * correo (alta y cambio de correo). Sin el, el codigo de 6 digitos de /verificar-correo —
     * que es publico — se puede probar por fuerza bruta y activar una cuenta registrada con el
     * correo de otra persona.
     */
    @JsonIgnore
    @Column(name = "intentos_codigo_verificacion")
    private Integer intentosCodigoVerificacion = 0;

    /**
     * Correo nuevo aun no confirmado (cambio de correo, admin o self-service). El correo real
     * ({@link #email}) NO se toca hasta que el codigo se valide correctamente - si el codigo
     * nunca se confirma (o falla), este campo se descarta y el email real nunca cambio.
     */
    @JsonIgnore
    @Column(name = "correo_pendiente")
    private String correoPendiente;

    /** true cuando la contrasena fue puesta por un ADMIN (reseteo) — obliga a cambiarla en el siguiente login. */
    @Column(name = "password_temporal")
    private Boolean passwordTemporal = Boolean.FALSE;

    /**
     * Momento del ultimo cambio de contrasena (self-service, restablecer por codigo, o reseteo
     * de ADMIN). El access token es stateless y no se puede revocar antes de sus 15 minutos de
     * vida - este campo es lo que permite rechazarlo igual: si el token fue emitido (iat) antes
     * de esta fecha, ya no es valido aunque su firma y expiracion sigan siendo correctas. Ver
     * {@link com.ventas.key.mis.productos.filter.JwtAuthenticationFilter}.
     */
    @Column(name = "password_actualizado_en")
    private LocalDateTime passwordActualizadoEn;

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

    /**
     * NO depende de correoVerificado a propósito: Spring Security evalua isEnabled() antes de
     * comparar la contrasena (DaoAuthenticationProvider.preAuthenticationChecks), asi que si
     * este metodo devolviera false por correo sin verificar, una contrasena incorrecta nunca
     * llegaria a validarse y siempre respondería "verifica tu correo" en vez de "credenciales
     * invalidas". El chequeo de correoVerificado (mejora 15) se hace aparte, en
     * AuthController.login(), despues de que la autenticacion ya paso.
     */
    @Override public boolean isEnabled() { return Boolean.TRUE.equals(enabled); }

    public boolean esAdmin() {
        return roles != null && "ROLE_ADMIN".equals(roles.getNombreRol());
    }

    @Override
    public String toString() {
        return "Usuario{username='" + username + "', email='" + email + "'}";
    }
}