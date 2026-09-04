package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Menu;
import com.ventas.key.mis.productos.entity.Submenu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Primera capa de tests de repository -- @DataJpaTest contra H2 en memoria (ver
// src/test/resources/application.properties, perfil "test" propio para no heredar el perfil
// "dev" que exige variables de entorno de BD real). Empieza por Submenu/Menu porque es el grafo
// de entidades mas simple de todo el sistema de permisos (Menu solo necesita "nombre"), y porque
// countByMenuId() es justo la query que sostiene la proteccion de SubmenuServiceImpl.delete()
// (no dejar un Menu con 0 pantallas) -- si esa query alguna vez cuenta mal, esa proteccion falla
// en silencio.
@DataJpaTest
@ActiveProfiles("test")
class ISubmenuRepositoryTest {

    @Autowired private ISubmenuRepository submenuRepository;
    @Autowired private IMenuRepository menuRepository;

    private Menu guardarMenu(String nombre) {
        Menu menu = new Menu();
        menu.setNombre(nombre);
        return menuRepository.save(menu);
    }

    private Submenu guardarSubmenu(Menu menu, String nombre, String ruta) {
        Submenu submenu = new Submenu();
        submenu.setMenu(menu);
        submenu.setNombre(nombre);
        submenu.setRuta(ruta);
        return submenuRepository.save(submenu);
    }

    @Test
    void findByMenuId_devuelveSoloLasPantallasDeEseMenu() {
        Menu catalogo = guardarMenu("Catálogo");
        Menu envios = guardarMenu("Envíos");
        guardarSubmenu(catalogo, "Modelos", "productos/buscar");
        guardarSubmenu(catalogo, "Agregar modelo", "productos/agregar");
        guardarSubmenu(envios, "Zonas de entrega", "lugares-entrega");

        List<Submenu> deCatalogo = submenuRepository.findByMenuId(catalogo.getId());

        assertThat(deCatalogo).extracting(Submenu::getRuta)
                .containsExactlyInAnyOrder("productos/buscar", "productos/agregar");
    }

    @Test
    void findByMenuId_devuelveVacioSiElMenuNoTienePantallas() {
        Menu vacio = guardarMenu("Sin pantallas todavía");

        List<Submenu> resultado = submenuRepository.findByMenuId(vacio.getId());

        assertThat(resultado).isEmpty();
    }

    @Test
    void countByMenuId_cuentaCorrectoParaLaProteccionDeSubmenuServiceImpl_delete() {
        Menu catalogo = guardarMenu("Catálogo");
        guardarSubmenu(catalogo, "Modelos", "productos/buscar");

        assertThat(submenuRepository.countByMenuId(catalogo.getId())).isEqualTo(1);

        guardarSubmenu(catalogo, "Agregar modelo", "productos/agregar");

        assertThat(submenuRepository.countByMenuId(catalogo.getId()))
                .as("con 2 pantallas, SubmenuServiceImpl.delete() debe permitir borrar una sin bloquear el grupo")
                .isEqualTo(2);
    }

    @Test
    void submenusSinGrupo_noSeCuentanParaNingunMenu() {
        // Home, Tienda, etc. -- Submenu.menu = null, no pertenecen a ningun acordeon.
        Submenu suelto = new Submenu();
        suelto.setMenu(null);
        suelto.setNombre("Home");
        suelto.setRuta("home");
        submenuRepository.save(suelto);

        Menu catalogo = guardarMenu("Catálogo");

        assertThat(submenuRepository.countByMenuId(catalogo.getId())).isZero();
    }

    @Test
    void guardaYRecuperaLaDescripcionDelBotonInfo() {
        // Campo agregado 2026-08-28 (ver migration_descripcion_submenu.sql) -- confirma que el
        // save generico (CrudAbstractServiceImpl) persiste este campo sin configuracion extra.
        Menu sistema = guardarMenu("Sistema");
        Submenu usuarios = guardarSubmenu(sistema, "Usuarios", "usuarios/buscar");
        usuarios.setDescripcion("Buscar y administrar las cuentas de usuario del sistema.");
        submenuRepository.save(usuarios);

        Submenu recuperado = submenuRepository.findById(usuarios.getId()).orElseThrow();

        assertThat(recuperado.getDescripcion()).isEqualTo("Buscar y administrar las cuentas de usuario del sistema.");
    }
}
