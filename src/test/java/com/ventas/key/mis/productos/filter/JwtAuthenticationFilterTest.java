package com.ventas.key.mis.productos.filter;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.PermisosEfectivosDto;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cubre autoridadesConPantallas() -- el metodo que traduce PermisosEfectivosDto en las
// GrantedAuthority que Spring Security de verdad evalua (SecurityConfig.hasAnyAuthority(...)).
// Es el puente entre "que pantallas/acciones tiene el usuario" y "que puede hacer un request real"
// -- si esto genera mal el formato de authority (prefijo/sufijo), SecurityConfig deja de
// reconocerlas y el usuario pierde acceso a todo aunque Gestion de roles diga que si lo tiene.
// Es un metodo PRIVADO -- se invoca via reflection (ReflectionTestUtils), patron estandar para
// probar logica interna en una clase que Spring instancia por @Component/@Autowired de campo,
// no por constructor.
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private UsuarioServiceImpl usuarioService;

    private JwtAuthenticationFilter filter;

    private JwtAuthenticationFilter nuevoFiltro() {
        JwtAuthenticationFilter f = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(f, "usuarioService", usuarioService);
        return f;
    }

    private Usuario usuarioConRol(Integer id, String nombreRol) {
        Roles rol = new Roles();
        rol.setId(1);
        rol.setNombreRol(nombreRol);
        rol.setPermisos(new HashSet<>());
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("empleado1");
        u.setRoles(rol);
        u.setPermisosExtra(new HashSet<>());
        return u;
    }

    private Submenu submenuConId(Integer id, String ruta) {
        Submenu s = new Submenu();
        s.setId(id);
        s.setRuta(ruta);
        return s;
    }

    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> invocar(Usuario usuario) {
        filter = nuevoFiltro();
        Object resultado = ReflectionTestUtils.invokeMethod(filter, "autoridadesConPantallas", usuario);
        return new HashSet<>((java.util.Collection<GrantedAuthority>) resultado);
    }

    @Test
    void generaLasAuthoritiesConElFormatoQueSecurityConfigEspera() {
        Usuario usuario = usuarioConRol(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "productos/buscar");
        AccionSubmenu habilitar = new AccionSubmenu();
        habilitar.setSubmenu(modelos);
        habilitar.setClave("habilitar");
        habilitar.setEtiqueta("Habilitar");

        when(usuarioService.permisosEfectivos(usuario)).thenReturn(new PermisosEfectivosDto(
                Set.of(modelos), Set.of(modelos), Set.of(habilitar)));

        Set<GrantedAuthority> authorities = invocar(usuario);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder(
                "ROLE_EMPLEADO",                       // authority base, la que ya trae userDetails.getAuthorities()
                "PANTALLA_productos/buscar",           // Ver
                "PANTALLA_productos/buscar_ESCRIBIR",  // Editar
                "PANTALLA_productos/buscar_ACCION_habilitar" // accion puntual
        );
    }

    @Test
    void pantallaSinEscrituraNiAcciones_soloGeneraLaAuthorityDeVer() {
        Usuario usuario = usuarioConRol(1, "ROLE_EMPLEADO");
        Submenu usuarios = submenuConId(20, "usuarios/buscar");

        when(usuarioService.permisosEfectivos(usuario)).thenReturn(new PermisosEfectivosDto(
                Set.of(usuarios), Set.of(), Set.of()));

        Set<GrantedAuthority> authorities = invocar(usuario);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_EMPLEADO", "PANTALLA_usuarios/buscar");
    }

    @Test
    void usuarioSinId_noConsultaPermisosYDevuelveSoloLaAuthorityBase() {
        // Defensivo -- en la práctica loadUserByUsername siempre trae un Usuario persistido con
        // id, pero el chequeo "usuario.getId() != null" existe en el código real y hay que
        // probar que efectivamente evita la consulta (no solo que "funciona igual").
        Usuario usuario = usuarioConRol(null, "ROLE_EMPLEADO");

        Set<GrantedAuthority> authorities = invocar(usuario);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_EMPLEADO");
        verify(usuarioService, never()).permisosEfectivos(any(Usuario.class));
    }

    @Test
    void siElCalculoDePermisosFalla_noPropagaLaExcepcionYDejaSoloLaAuthorityBase() {
        // "Fail open a la authority base" a proposito (ver catch en autoridadesConPantallas): un
        // fallo puntual calculando pantallas no debe tumbar la autenticacion completa del
        // request -- el usuario simplemente se queda sin las authorities extra esa vez, no sin
        // poder autenticarse.
        Usuario usuario = usuarioConRol(1, "ROLE_EMPLEADO");
        when(usuarioService.permisosEfectivos(usuario)).thenThrow(new RuntimeException("boom"));

        Set<GrantedAuthority> authorities = invocar(usuario);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_EMPLEADO");
    }
}
