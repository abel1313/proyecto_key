package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.UsuarioSubmenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PermisosEfectivosDto;
import com.ventas.key.mis.productos.repository.IPermisoRepository;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IUsuarioSubmenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Cubre permisosEfectivos() -- el calculo que arma lo que va al claim "pantallas" del JWT (ver
// PermisosEfectivosDto, JwtAuthenticationFilter, AuthController.login/refresh). La formula real
// (PLAN_PERMISOS_PANTALLAS.md seccion 3) es:
//   pantallas efectivas = (pantallas del ROL) U {excepciones concedido=true} - {concedido=false}
// pantallasEscritura y acciones NO tienen excepcion por usuario todavia -- salen solo del rol.
// Este calculo es justo el que se optimizo 2 veces esta sesion (round 1: 3 fetches -> 1; round 2:
// dejo de re-pedir el Usuario que el caller ya tenia) -- un test que verifique el RESULTADO,
// no la cantidad de queries, protege que una futura optimizacion no rompa la formula por accidente.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IRolRepository rolRepository;
    @Mock private IPermisoRepository permisoRepository;
    @Mock private ISubmenuRepository submenuRepository;
    @Mock private IUsuarioSubmenuRepository usuarioSubmenuRepository;
    @Mock private UsuarioVerificacionService usuarioVerificacionService;
    @Mock private SesionRefreshService sesionRefreshService;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, new ErrorGenerico(), usuarioRepository,
                passwordEncoder, rolRepository, permisoRepository, submenuRepository,
                usuarioSubmenuRepository, usuarioVerificacionService, sesionRefreshService);
    }

    private Submenu submenuConId(Integer id, String ruta) {
        Submenu s = new Submenu();
        s.setId(id);
        s.setNombre(ruta);
        s.setRuta(ruta);
        return s;
    }

    private Usuario usuarioConRol(Integer id, Roles rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRoles(rol);
        return u;
    }

    private Roles rolConPermisos(Set<Submenu> submenus, Set<Submenu> submenusEscritura, Set<AccionSubmenu> acciones) {
        Roles rol = new Roles();
        rol.setId(1);
        rol.setNombreRol("ROLE_EMPLEADO");
        rol.setSubmenus(submenus);
        rol.setSubmenusEscritura(submenusEscritura);
        rol.setAcciones(acciones);
        return rol;
    }

    private UsuarioSubmenu excepcion(Submenu submenu, boolean concedido) {
        UsuarioSubmenu e = new UsuarioSubmenu();
        e.setSubmenu(submenu);
        e.setConcedido(concedido);
        return e;
    }

    // ── permisosEfectivos(Usuario) -- version que reusa el objeto ya cargado ────────────────

    @Test
    void permisosEfectivos_usuarioSinRol_devuelveTodoVacioSinExplotar() {
        Usuario usuario = usuarioConRol(1, null);
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of());

        PermisosEfectivosDto dto = service.permisosEfectivos(usuario);

        assertThat(dto.getPantallas()).isEmpty();
        assertThat(dto.getPantallasEscritura()).isEmpty();
        assertThat(dto.getAcciones()).isEmpty();
    }

    @Test
    void permisosEfectivos_sinExcepciones_reflejaExactamenteLoDelRol() {
        Submenu modelos = submenuConId(10, "productos/buscar");
        Submenu usuarios = submenuConId(20, "usuarios/buscar");
        AccionSubmenu habilitar = new AccionSubmenu();
        habilitar.setId(100);
        habilitar.setSubmenu(modelos);
        habilitar.setClave("habilitar");
        habilitar.setEtiqueta("Habilitar");

        Roles rol = rolConPermisos(
                new HashSet<>(Set.of(modelos, usuarios)),
                new HashSet<>(Set.of(modelos)),
                new HashSet<>(Set.of(habilitar)));
        Usuario usuario = usuarioConRol(1, rol);
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of());

        PermisosEfectivosDto dto = service.permisosEfectivos(usuario);

        assertThat(dto.getPantallas()).containsExactlyInAnyOrder(modelos, usuarios);
        assertThat(dto.getPantallasEscritura()).containsExactly(modelos);
        assertThat(dto.getAcciones()).containsExactly(habilitar);
    }

    @Test
    void permisosEfectivos_excepcionConcedida_sumaUnaPantallaQueElRolNoDaba() {
        Submenu modelos = submenuConId(10, "productos/buscar");
        Submenu usuarios = submenuConId(20, "usuarios/buscar"); // el rol NO la tiene
        Roles rol = rolConPermisos(new HashSet<>(Set.of(modelos)), new HashSet<>(), new HashSet<>());
        Usuario usuario = usuarioConRol(1, rol);
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of(excepcion(usuarios, true)));

        PermisosEfectivosDto dto = service.permisosEfectivos(usuario);

        assertThat(dto.getPantallas()).containsExactlyInAnyOrder(modelos, usuarios);
    }

    @Test
    void permisosEfectivos_excepcionRevocada_ganaSobreLoQueDaElRol() {
        Submenu modelos = submenuConId(10, "productos/buscar");
        Submenu usuarios = submenuConId(20, "usuarios/buscar");
        Roles rol = rolConPermisos(new HashSet<>(Set.of(modelos, usuarios)), new HashSet<>(), new HashSet<>());
        Usuario usuario = usuarioConRol(1, rol);
        // El rol SI da "usuarios/buscar", pero a este usuario puntual se la revocaron.
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of(excepcion(usuarios, false)));

        PermisosEfectivosDto dto = service.permisosEfectivos(usuario);

        assertThat(dto.getPantallas()).containsExactly(modelos);
    }

    @Test
    void permisosEfectivos_noDuplicaCuandoLaExcepcionConcedidaEsUnaPantallaQueYaTeniaElRol() {
        // Misma instancia en ambos lados a proposito: en produccion, dentro de un mismo
        // EntityManager, Hibernate devuelve el MISMO objeto Java para el mismo id (identity map)
        // -- Submenu no tiene equals/hashCode propio (usa el de Object), asi que el Set solo
        // deduplica bien si ambos lados apuntan a la misma instancia. Este test documenta esa
        // dependencia -- si algun dia se guarda la excepcion con una instancia DISTINTA del
        // mismo id (ej. viniendo de una query separada sin cache de primer nivel), este mismo
        // caso dejaria de deduplicar y el front recibiria pantallas repetidas en el JWT.
        Submenu modelos = submenuConId(10, "productos/buscar");
        Roles rol = rolConPermisos(new HashSet<>(Set.of(modelos)), new HashSet<>(), new HashSet<>());
        Usuario usuario = usuarioConRol(1, rol);
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of(excepcion(modelos, true)));

        PermisosEfectivosDto dto = service.permisosEfectivos(usuario);

        assertThat(dto.getPantallas()).hasSize(1).containsExactly(modelos);
    }

    // ── permisosEfectivos(Integer) -- version que hace su propio fetch ──────────────────────

    @Test
    void permisosEfectivosPorId_delegaEnLaVersionConUsuarioYaCargado() {
        Submenu modelos = submenuConId(10, "productos/buscar");
        Roles rol = rolConPermisos(new HashSet<>(Set.of(modelos)), new HashSet<>(), new HashSet<>());
        Usuario usuario = usuarioConRol(1, rol);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioSubmenuRepository.findByUsuarioId(1)).thenReturn(List.of());

        PermisosEfectivosDto dto = service.permisosEfectivos((Integer) 1);

        assertThat(dto.getPantallas()).containsExactly(modelos);
    }

    @Test
    void permisosEfectivosPorId_usuarioInexistente_lanzaExceptionDataNotFound() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.permisosEfectivos((Integer) 99))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    // ── submenusEfectivos(Integer) -- misma formula, metodo standalone (JwtAuthenticationFilter
    // ya no lo usa directo, pero sigue expuesto -- si diverge de permisosEfectivos() alguien va a
    // ver pantallas distintas segun que endpoint pregunte) ──────────────────────────────────

    @Test
    void submenusEfectivos_aplicaLaMismaFormulaQuePermisosEfectivos() {
        Submenu modelos = submenuConId(10, "productos/buscar");
        Submenu usuarios = submenuConId(20, "usuarios/buscar");
        Submenu clientes = submenuConId(30, "clientes/buscar");
        Roles rol = rolConPermisos(new HashSet<>(Set.of(modelos, usuarios)), new HashSet<>(), new HashSet<>());
        Usuario usuario = usuarioConRol(1, rol);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioSubmenuRepository.findByUsuarioId(1))
                .thenReturn(List.of(excepcion(clientes, true), excepcion(usuarios, false)));

        Set<Submenu> efectivas = service.submenusEfectivos(1);

        assertThat(efectivas).containsExactlyInAnyOrder(modelos, clientes);
    }
}
