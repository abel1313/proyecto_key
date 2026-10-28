package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfigurarAceptaMsiCasoUso;
import com.ventas.key.mis.productos.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La casilla de meses sin intereses: solo la maneja el administrador y los errores salen con su código. */
class AceptaMsiControllerTest {

    private final ConfigurarAceptaMsiCasoUso casillas = mock(ConfigurarAceptaMsiCasoUso.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new AceptaMsiController(casillas)).build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void entrar(String rol) {
        Usuario u = new Usuario();
        u.setUsername("alguien");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u, "t", List.of(new SimpleGrantedAuthority(rol))));
    }

    @Test
    void elAdminLeeLaCasillaDeUnProducto() throws Exception {
        entrar("ROLE_ADMIN");
        when(casillas.aceptaProducto(5)).thenReturn(true);
        mvc.perform(get("/v1/acepta-msi/producto/5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aceptaMsi").value(true));
    }

    @Test
    void unClienteNoPuedeLeerNiCambiarNada_yRecibeNoEncontrado() throws Exception {
        entrar("ROLE_USER");
        mvc.perform(get("/v1/acepta-msi/producto/5")).andExpect(status().isNotFound());
        mvc.perform(put("/v1/acepta-msi/producto/5").contentType(MediaType.APPLICATION_JSON).content("{\"aceptaMsi\":true}"))
                .andExpect(status().isNotFound());
        verify(casillas, never()).cambiarProducto(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void elAdminActivaLaCasillaDeUnProducto() throws Exception {
        entrar("ROLE_ADMIN");
        mvc.perform(put("/v1/acepta-msi/producto/5").contentType(MediaType.APPLICATION_JSON).content("{\"aceptaMsi\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.aceptaMsi").value(true));
        verify(casillas).cambiarProducto(5, true);
    }

    @Test
    void unProductoNoAdmiteNull_peroUnArticuloSi_queSignificaHeredar() throws Exception {
        entrar("ROLE_ADMIN");
        mvc.perform(put("/v1/acepta-msi/producto/5").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        when(casillas.articulo(7)).thenReturn(new ConfigMsiArticulo(null, true));
        mvc.perform(put("/v1/acepta-msi/articulo/7").contentType(MediaType.APPLICATION_JSON).content("{\"aceptaMsi\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heredaDelProducto").value(true))
                .andExpect(jsonPath("$.data.efectivo").value(true));
        verify(casillas).cambiarArticulo(7, null);
    }

    @Test
    void elArticuloDiceSuCasillaPropiaLaDelProductoYLaEfectiva() throws Exception {
        entrar("ROLE_ADMIN");
        when(casillas.articulo(7)).thenReturn(new ConfigMsiArticulo(false, true));
        mvc.perform(get("/v1/acepta-msi/articulo/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.propio").value(false))
                .andExpect(jsonPath("$.data.delProducto").value(true))
                .andExpect(jsonPath("$.data.efectivo").value(false));
    }

    @Test
    void lasListasPintanElChipConElValorEfectivoDeVariosArticulos() throws Exception {
        entrar("ROLE_ADMIN");
        when(casillas.efectivos(List.of(1, 2))).thenReturn(Map.of(1, true, 2, false));
        mvc.perform(get("/v1/acepta-msi/articulos?ids=1,2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data['1']").value(true))
                .andExpect(jsonPath("$.data['2']").value(false));
    }

    @Test
    void codigosDeError_404SiNoExiste_409SiFaltaLaMigracion() throws Exception {
        entrar("ROLE_ADMIN");
        when(casillas.aceptaProducto(99)).thenThrow(PagoConTarjetaException.productoNoExiste(99));
        mvc.perform(get("/v1/acepta-msi/producto/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el producto 99"));
        doThrow(PagoConTarjetaException.casillaMsiNoDisponible()).when(casillas).cambiarProducto(5, true);
        mvc.perform(put("/v1/acepta-msi/producto/5").contentType(MediaType.APPLICATION_JSON).content("{\"aceptaMsi\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("migration_pago_tarjeta.sql")));
    }
}
