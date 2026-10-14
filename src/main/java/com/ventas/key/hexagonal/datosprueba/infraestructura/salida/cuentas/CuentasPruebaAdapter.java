package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.cuentas;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.AccionDePantalla;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.NombresDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.CuentasPruebaPort;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.service.RolesServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R23: el usuario de prueba con un rol limitado. Los permisos del rol se dan con los mismos métodos que
 * Gestión de roles ({@code RolesServiceImpl.agregarSubmenu} / {@code agregarAccion}, que validan igual que la
 * pantalla). La contraseña es aleatoria y no se guarda ni se muestra en ningún lado: el administrador le pone
 * una con "Resetear contraseña" en la pantalla de Usuarios. Nunca se borra nada: al quitar la prueba el usuario
 * solo se desactiva.
 */
@Component
@RequiredArgsConstructor
public class CuentasPruebaAdapter implements CuentasPruebaPort {

    private static final SecureRandom AZAR = new SecureRandom();

    private final IRolRepository roles;
    private final RolesServiceImpl gestionDeRoles;
    private final IUsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Override
    public String usuarioConRol(String prueba, List<String> pantallas, List<AccionDePantalla> acciones) {
        Roles rol = rolConPermisosExactos(NombresDePrueba.rol(prueba), pantallas, acciones);

        String username = NombresDePrueba.usuario(prueba);
        Usuario usuario = usuarios.findByUsername(username).orElseGet(Usuario::new);
        LocalDateTime ahora = LocalDateTime.now();
        usuario.setUsername(username);
        usuario.setEmail(NombresDePrueba.correoUsuario(prueba));
        if (usuario.getPassword() == null) {
            usuario.setPassword(passwordEncoder.encode(contrasenaQueNadieConoce()));
        }
        usuario.setRoles(rol);
        usuario.setEnabled(true);
        usuario.setCorreoVerificado(true);
        usuario.setAceptoTerminos(true);
        usuario.setFechaAceptoTerminos(ahora);
        usuario.setAceptoPrivacidad(true);
        usuario.setFechaAceptoPrivacidad(ahora);
        usuarios.save(usuario);
        return username;
    }

    @Override
    public int quitarUsuarioDeLaPrueba(String prueba) {
        return usuarios.findByUsername(NombresDePrueba.usuario(prueba))
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .map(u -> {
                    u.setEnabled(false);
                    usuarios.save(u);
                    return 1;
                })
                .orElse(0);
    }

    /** El rol con solo esas pantallas y acciones: si ya existía, se le quita todo y se le vuelve a dar. */
    private Roles rolConPermisosExactos(String nombreRol, List<String> pantallas, List<AccionDePantalla> acciones) {
        Roles rol = roles.findByNombreRol(nombreRol).orElseGet(() -> {
            Roles nuevo = new Roles();
            nuevo.setNombreRol(nombreRol);
            return roles.save(nuevo);
        });
        rol.getSubmenus().clear();
        rol.getSubmenusEscritura().clear();
        rol.getAcciones().clear();
        rol.getPermisos().clear();
        roles.save(rol);
        for (String ruta : pantallas) {
            gestionDeRoles.agregarSubmenu(rol.getId(), pantalla(ruta));
        }
        for (AccionDePantalla accion : acciones) {
            gestionDeRoles.agregarAccion(rol.getId(), accion(accion));
        }
        return roles.findById(rol.getId()).orElseThrow();
    }

    private int pantalla(String ruta) {
        List<Integer> ids = jdbc.queryForList("SELECT id FROM submenu WHERE ruta = ? ORDER BY id LIMIT 1", Integer.class, ruta);
        if (ids.isEmpty()) {
            throw new IllegalStateException("No existe la pantalla \"" + ruta + "\" en el menú de esta base");
        }
        return ids.get(0);
    }

    private int accion(AccionDePantalla accion) {
        List<Integer> ids = jdbc.queryForList("SELECT a.id FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id "
                + "WHERE s.ruta = ? AND a.clave = ? ORDER BY a.id LIMIT 1", Integer.class, accion.ruta(), accion.clave());
        if (ids.isEmpty()) {
            throw new IllegalStateException("No existe la acción \"" + accion.clave() + "\" de \"" + accion.ruta()
                    + "\" en esta base (¿falta correr su migración?)");
        }
        return ids.get(0);
    }

    private static String contrasenaQueNadieConoce() {
        byte[] bytes = new byte[24];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
