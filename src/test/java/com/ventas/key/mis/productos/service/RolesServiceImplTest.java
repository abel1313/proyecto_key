package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.repository.IAccionSubmenuRepository;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Cubre los invariantes de permisos que RolesServiceImpl garantiza "a mano" (no la BD), tal como
// dicen sus propios comentarios: un submenu en submenusEscritura/acciones SIEMPRE debe estar
// primero en submenus, y quitar el Ver de una pantalla debe arrastrar (cascada) su Editar y sus
// acciones puntuales -- sin esto, un rol podia quedar con "Editar Modelos" sin poder ver Modelos.
// Estos invariantes son los que sostienen todo el sistema de permisos armado en esta sesión
// (Fase 1/2/3, ver PLAN_PERMISOS_PANTALLAS.md), así que son el primer lugar donde un test paga
// la pena: si alguien los rompe sin querer, esto lo agarra antes que un usuario en producción.
@ExtendWith(MockitoExtension.class)
class RolesServiceImplTest {

    @Mock private IRolRepository rolRepository;
    @Mock private ISubmenuRepository submenuRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IAccionSubmenuRepository accionSubmenuRepository;

    private RolesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RolesServiceImpl(rolRepository, new ErrorGenerico(), submenuRepository,
                usuarioRepository, accionSubmenuRepository);
    }

    private Roles rolConId(Integer id, String nombreRol) {
        Roles rol = new Roles();
        rol.setId(id);
        rol.setNombreRol(nombreRol);
        rol.setSubmenus(new HashSet<>());
        rol.setSubmenusEscritura(new HashSet<>());
        rol.setAcciones(new HashSet<>());
        return rol;
    }

    private Submenu submenuConId(Integer id, String nombre, String ruta) {
        Submenu s = new Submenu();
        s.setId(id);
        s.setNombre(nombre);
        s.setRuta(ruta);
        return s;
    }

    private AccionSubmenu accionDe(Integer id, Submenu submenu, String clave) {
        AccionSubmenu a = new AccionSubmenu();
        a.setId(id);
        a.setSubmenu(submenu);
        a.setClave(clave);
        a.setEtiqueta(clave);
        return a;
    }

    // ── agregarSubmenu / quitarSubmenu ──────────────────────────────────────

    @Test
    void agregarSubmenu_agregaLaPantallaAlRol() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu submenu = submenuConId(10, "Modelos", "productos/buscar");
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(submenuRepository.findById(10)).thenReturn(Optional.of(submenu));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.agregarSubmenu(1, 10);

        assertThat(resultado.getSubmenus()).contains(submenu);
    }

    @Test
    void quitarSubmenu_arrastraEnCascadaEscrituraYAcciones() {
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        rol.getSubmenus().add(modelos);
        rol.getSubmenusEscritura().add(modelos);
        AccionSubmenu habilitar = accionDe(100, modelos, "habilitar");
        rol.getAcciones().add(habilitar);

        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(submenuRepository.findById(10)).thenReturn(Optional.of(modelos));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.quitarSubmenu(1, 10);

        assertThat(resultado.getSubmenus()).isEmpty();
        assertThat(resultado.getSubmenusEscritura())
                .as("quitar el Ver debe arrastrar el Editar de la misma pantalla")
                .isEmpty();
        assertThat(resultado.getAcciones())
                .as("quitar el Ver debe arrastrar las acciones puntuales de la misma pantalla")
                .isEmpty();
    }

    @Test
    void quitarSubmenu_noDejaHuerfanasLasAccionesDeOtraPantalla() {
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        Submenu usuarios = submenuConId(20, "Usuarios", "usuarios/buscar");
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        rol.getSubmenus().add(modelos);
        rol.getSubmenus().add(usuarios);
        AccionSubmenu accionUsuarios = accionDe(200, usuarios, "buscar");
        rol.getAcciones().add(accionUsuarios);

        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(submenuRepository.findById(10)).thenReturn(Optional.of(modelos));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.quitarSubmenu(1, 10);

        assertThat(resultado.getAcciones())
                .as("quitar Modelos no debe tocar una acción que pertenece a Usuarios")
                .containsExactly(accionUsuarios);
    }

    @Test
    void quitarSubmenu_noPermiteQuitarleAAdminUnaPantallaProtegida() {
        Roles admin = rolConId(1, "ROLE_ADMIN");
        Submenu gestionRoles = submenuConId(30, "Gestión de roles", "gestion-menu/roles");
        admin.getSubmenus().add(gestionRoles);

        when(rolRepository.findById(1)).thenReturn(Optional.of(admin));
        when(submenuRepository.findById(30)).thenReturn(Optional.of(gestionRoles));

        assertThatThrownBy(() -> service.quitarSubmenu(1, 30))
                .isInstanceOf(ExceptionOperacionNoPermitida.class);
        verify(rolRepository, never()).save(any());
    }

    @Test
    void quitarSubmenu_siPermiteQuitarleAOtroRolUnaPantallaProtegidaDeAdmin() {
        // La protección es solo para ROLE_ADMIN -- cualquier otro rol puede perder
        // "gestion-menu/roles" sin problema (de hecho, la mayoría nunca la tiene).
        Roles empleado = rolConId(2, "ROLE_EMPLEADO");
        Submenu gestionRoles = submenuConId(30, "Gestión de roles", "gestion-menu/roles");
        empleado.getSubmenus().add(gestionRoles);

        when(rolRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(submenuRepository.findById(30)).thenReturn(Optional.of(gestionRoles));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.quitarSubmenu(2, 30);

        assertThat(resultado.getSubmenus()).isEmpty();
    }

    // ── agregarSubmenuEscritura ──────────────────────────────────────────────

    @Test
    void agregarSubmenuEscritura_rechazaSiElRolNoPuedeVerLaPantalla() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(submenuRepository.findById(10)).thenReturn(Optional.of(modelos));

        assertThatThrownBy(() -> service.agregarSubmenuEscritura(1, 10))
                .isInstanceOf(ExceptionOperacionNoPermitida.class)
                .hasMessageContaining("primero necesita poder VER");
        verify(rolRepository, never()).save(any());
    }

    @Test
    void agregarSubmenuEscritura_permiteSiElRolYaPuedeVerLaPantalla() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        rol.getSubmenus().add(modelos);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(submenuRepository.findById(10)).thenReturn(Optional.of(modelos));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.agregarSubmenuEscritura(1, 10);

        assertThat(resultado.getSubmenusEscritura()).contains(modelos);
    }

    // ── agregarAccion / quitarAccion (Fase 3) ────────────────────────────────

    @Test
    void agregarAccion_rechazaSiElRolNoPuedeVerLaPantallaDeEsaAccion() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        AccionSubmenu habilitar = accionDe(100, modelos, "habilitar");
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(accionSubmenuRepository.findById(100)).thenReturn(Optional.of(habilitar));

        assertThatThrownBy(() -> service.agregarAccion(1, 100))
                .isInstanceOf(ExceptionOperacionNoPermitida.class)
                .hasMessageContaining("primero necesita poder VER");
        verify(rolRepository, never()).save(any());
    }

    @Test
    void agregarAccion_permiteSiElRolYaPuedeVerLaPantallaDeEsaAccion() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        rol.getSubmenus().add(modelos);
        AccionSubmenu habilitar = accionDe(100, modelos, "habilitar");
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(accionSubmenuRepository.findById(100)).thenReturn(Optional.of(habilitar));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.agregarAccion(1, 100);

        assertThat(resultado.getAcciones()).contains(habilitar);
    }

    @Test
    void quitarAccion_quitaSoloLaAccionIndicada() {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        Submenu modelos = submenuConId(10, "Modelos", "productos/buscar");
        AccionSubmenu habilitar = accionDe(100, modelos, "habilitar");
        AccionSubmenu eliminar = accionDe(101, modelos, "eliminar");
        rol.getAcciones().add(habilitar);
        rol.getAcciones().add(eliminar);

        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(accionSubmenuRepository.findById(100)).thenReturn(Optional.of(habilitar));
        when(rolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Roles resultado = service.quitarAccion(1, 100);

        assertThat(resultado.getAcciones()).containsExactly(eliminar);
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_rechazaBorrarRoleAdmin() {
        Roles admin = rolConId(1, "ROLE_ADMIN");
        when(rolRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(ExceptionOperacionNoPermitida.class);
        verify(rolRepository, never()).delete(any());
    }

    @Test
    void delete_rechazaBorrarUnRolConUsuariosAsignados() {
        Roles rol = rolConId(2, "ROLE_EMPLEADO");
        when(rolRepository.findById(2)).thenReturn(Optional.of(rol));
        when(usuarioRepository.countByRolesId(2)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(2))
                .isInstanceOf(ExceptionOperacionNoPermitida.class)
                .hasMessageContaining("3 usuario");
        verify(rolRepository, never()).delete(any());
    }

    @Test
    void delete_borraUnRolSinUsuariosAsignados() throws Exception {
        Roles rol = rolConId(2, "ROLE_EMPLEADO");
        when(rolRepository.findById(2)).thenReturn(Optional.of(rol));
        when(usuarioRepository.countByRolesId(2)).thenReturn(0L);

        Roles resultado = service.delete(2);

        assertThat(resultado).isEqualTo(rol);
        verify(rolRepository).delete(rol);
    }

    @Test
    void quitarSubmenu_lanzaExceptionDataNotFoundSiElRolNoExiste() {
        when(rolRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.quitarSubmenu(99, 1))
                .isInstanceOf(ExceptionDataNotFound.class);
    }
}
