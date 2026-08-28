package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.service.RolesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cubre los 6 endpoints propios de RolesController (asignar/quitar Ver, Editar y acciones
// puntuales a un rol -- todo lo de Fase 1/2/3 de permisos). No usa @SpringBootTest ni contexto
// real: MockMvc "standalone" solo sobre este controller, con el service mockeado -- rapido, sin
// BD, y suficiente porque lo que hay que verificar es el mapeo HTTP (path variables -> llamada al
// service -> 200 con el body correcto, o 400 con el mensaje de la excepcion), no la logica de
// negocio en si (esa ya la cubre RolesServiceImplTest).
//
// El patron try/catch -> 400 con e.getMessage() en "mensaje" se repite igual en casi todos los
// controllers de este proyecto (ver AbstractController y los controllers custom) -- este test
// deja probado una vez que ese patron funciona como se espera para el caso mas nuevo/fragil
// (Fase 3, agregado 2026-08-27).
@ExtendWith(MockitoExtension.class)
class RolesControllerTest {

    @Mock private RolesServiceImpl service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RolesController(service)).build();
    }

    private Roles rolConId(Integer id, String nombreRol) {
        Roles rol = new Roles();
        rol.setId(id);
        rol.setNombreRol(nombreRol);
        return rol;
    }

    @Test
    void agregarSubmenu_devuelve200ConElRolActualizado() throws Exception {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        when(service.agregarSubmenu(1, 10)).thenReturn(rol);

        mockMvc.perform(post("/v1/roles/{rolId}/submenus/{submenuId}", 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nombreRol").value("ROLE_EMPLEADO"));
    }

    @Test
    void quitarSubmenu_siElServiceRechazaPorPantallaProtegida_devuelve400ConElMensaje() throws Exception {
        when(service.quitarSubmenu(1, 30)).thenThrow(
                new ExceptionOperacionNoPermitida("No se le puede quitar a ROLE_ADMIN el acceso a \"Gestión de roles\""));

        mockMvc.perform(delete("/v1/roles/{rolId}/submenus/{submenuId}", 1, 30))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se le puede quitar a ROLE_ADMIN el acceso a \"Gestión de roles\""));
    }

    @Test
    void agregarSubmenuEscritura_devuelve200ConElRolActualizado() throws Exception {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        when(service.agregarSubmenuEscritura(1, 10)).thenReturn(rol);

        mockMvc.perform(post("/v1/roles/{rolId}/submenus/{submenuId}/escritura", 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void agregarSubmenuEscritura_siElServiceRechazaPorFaltarVer_devuelve400ConElMensaje() throws Exception {
        when(service.agregarSubmenuEscritura(1, 10)).thenThrow(
                new ExceptionOperacionNoPermitida("\"ROLE_EMPLEADO\" primero necesita poder VER \"Modelos\" antes de poder escribir en ella."));

        mockMvc.perform(post("/v1/roles/{rolId}/submenus/{submenuId}/escritura", 1, 10))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "\"ROLE_EMPLEADO\" primero necesita poder VER \"Modelos\" antes de poder escribir en ella."));
    }

    @Test
    void quitarSubmenuEscritura_devuelve200ConElRolActualizado() throws Exception {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        when(service.quitarSubmenuEscritura(1, 10)).thenReturn(rol);

        mockMvc.perform(delete("/v1/roles/{rolId}/submenus/{submenuId}/escritura", 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void agregarAccion_devuelve200ConElRolActualizado() throws Exception {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        when(service.agregarAccion(1, 100)).thenReturn(rol);

        mockMvc.perform(post("/v1/roles/{rolId}/acciones/{accionId}", 1, 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void agregarAccion_siElServiceRechazaPorFaltarVer_devuelve400ConElMensaje() throws Exception {
        when(service.agregarAccion(1, 100)).thenThrow(
                new ExceptionOperacionNoPermitida("\"ROLE_EMPLEADO\" primero necesita poder VER \"Modelos\" antes de poder usar la acción \"Habilitar\"."));

        mockMvc.perform(post("/v1/roles/{rolId}/acciones/{accionId}", 1, 100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "\"ROLE_EMPLEADO\" primero necesita poder VER \"Modelos\" antes de poder usar la acción \"Habilitar\"."));
    }

    @Test
    void quitarAccion_devuelve200ConElRolActualizado() throws Exception {
        Roles rol = rolConId(1, "ROLE_EMPLEADO");
        when(service.quitarAccion(1, 100)).thenReturn(rol);

        mockMvc.perform(delete("/v1/roles/{rolId}/acciones/{accionId}", 1, 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
