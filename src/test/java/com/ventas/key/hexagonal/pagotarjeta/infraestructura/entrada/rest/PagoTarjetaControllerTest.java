package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.CancelarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.DevolverCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.IniciarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.OfrecerPlanesCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.service.CacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quien puede hacer que en el cobro con tarjeta, y como se traducen los errores a codigos HTTP. */
class PagoTarjetaControllerTest {

    private final IniciarCobroCasoUso iniciar = mock(IniciarCobroCasoUso.class);
    private final ConfirmarCobroCasoUso confirmar = mock(ConfirmarCobroCasoUso.class);
    private final CancelarCobroCasoUso cancelar = mock(CancelarCobroCasoUso.class);
    private final DevolverCobroCasoUso devolver = mock(DevolverCobroCasoUso.class);
    private final OfrecerPlanesCasoUso planes = mock(OfrecerPlanesCasoUso.class);
    private final IntentosDePagoPort intentos = mock(IntentosDePagoPort.class);
    private final PedidoACobrarPort pedidos = mock(PedidoACobrarPort.class);
    private final CacheService cache = mock(CacheService.class);
    private MockMvc mvc;

    private static final Instant AHORA = Instant.parse("2026-10-22T18:00:00Z");

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(
                new PagoTarjetaController(iniciar, confirmar, cancelar, devolver, planes, intentos, pedidos, cache)).build();
        when(pedidos.buscar(10)).thenReturn(Optional.of(pedido(10, 5)));          // pedido del cliente 5
        when(pedidos.buscar(11)).thenReturn(Optional.of(pedido(11, 6)));          // pedido del cliente 6
        when(pedidos.buscar(12)).thenReturn(Optional.of(pedido(12, null)));       // pedido sin cliente registrado
        when(intentos.buscar(1L)).thenReturn(Optional.of(intento(1L, 10, 5)));
        when(intentos.buscar(2L)).thenReturn(Optional.of(intento(2L, 11, 6)));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static PedidoACobrar pedido(int id, Integer clienteId) {
        return new PedidoACobrar(id, clienteId, TipoDeCobro.CONTADO, new BigDecimal("450"), false, false, false, null, List.of());
    }

    private static IntentoDePago intento(Long id, int pedidoId, Integer clienteId) {
        return IntentoDePago.nuevo(pedidoId, clienteId, CanalDePago.EN_LINEA, new BigDecimal("450"), 1, "x", AHORA).conId(id);
    }

    private static void entrar(String rol, Integer clienteId) {
        Usuario u = new Usuario();
        u.setUsername(clienteId == null ? "admin" : "cliente" + clienteId);
        if (clienteId != null) {
            Cliente c = new Cliente();
            c.setId(clienteId);
            u.setCliente(c);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u, "t", List.of(new SimpleGrantedAuthority(rol))));
    }

    private static final String COBRAR_TERMINAL = "{\"pedidoId\":10,\"canal\":\"TERMINAL\",\"cuotas\":1}";

    @Test
    void elAdminCobraEnLaTerminalYElUsuarioQuedaDelToken() throws Exception {
        entrar("ROLE_ADMIN", null);
        when(iniciar.iniciar(eq(10), eq(CanalDePago.TERMINAL), eq(1), eq("admin"))).thenReturn(intento(7L, 10, 5));
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.estado").value("CREADO"))
                .andExpect(jsonPath("$.data.estadoTexto").value("creado"))
                .andExpect(jsonPath("$.data.enProceso").value(true));
    }

    @Test
    void unClienteNoPuedeUsarLaTerminal_404() throws Exception {
        entrar("ROLE_USER", 5);
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isNotFound());
        verify(iniciar, never()).iniciar(anyInt(), any(), anyInt(), any());
    }

    @Test
    void unClientePagaEnLineaSoloSuPedido() throws Exception {
        entrar("ROLE_USER", 5);
        when(iniciar.iniciar(eq(10), eq(CanalDePago.EN_LINEA), eq(1), eq("cliente5"))).thenReturn(intento(7L, 10, 5));
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":10,\"canal\":\"EN_LINEA\",\"cuotas\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":11,\"canal\":\"EN_LINEA\",\"cuotas\":1}"))      // pedido de otro cliente
                .andExpect(status().isNotFound());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":12,\"canal\":\"EN_LINEA\",\"cuotas\":1}"))      // sin dueño registrado: solo admin
                .andExpect(status().isNotFound());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":999,\"canal\":\"EN_LINEA\",\"cuotas\":1}"))     // no existe: mismo 404
                .andExpect(status().isNotFound());
    }

    @Test
    void unUsuarioSinClienteNoPuedeCobrarPedidosSinDueño_noSeComparaNullConNull() throws Exception {
        entrar("ROLE_USER", null);                                                        // sin cliente ligado
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":12,\"canal\":\"EN_LINEA\",\"cuotas\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarYCancelarSoloElDueñoOElAdmin() throws Exception {
        entrar("ROLE_USER", 5);
        when(confirmar.confirmar(1L)).thenReturn(intento(1L, 10, 5));
        mvc.perform(get("/v1/pagos-tarjeta/1")).andExpect(status().isOk());
        mvc.perform(get("/v1/pagos-tarjeta/2")).andExpect(status().isNotFound());
        mvc.perform(post("/v1/pagos-tarjeta/2/cancelar")).andExpect(status().isNotFound());
        verify(confirmar, never()).confirmar(2L);
        verify(cancelar, never()).cancelar(2L);
    }

    @Test
    void devolverEsSoloDelAdmin() throws Exception {
        entrar("ROLE_USER", 5);
        mvc.perform(post("/v1/pagos-tarjeta/1/devolver")).andExpect(status().isNotFound());
        verify(devolver, never()).devolver(anyLong(), any(), any());

        entrar("ROLE_ADMIN", null);
        when(devolver.devolver(eq(1L), eq(new BigDecimal("50")), eq("admin"))).thenReturn(intento(1L, 10, 5));
        mvc.perform(post("/v1/pagos-tarjeta/1/devolver").contentType(MediaType.APPLICATION_JSON).content("{\"monto\":50}"))
                .andExpect(status().isOk());
    }

    @Test
    void losErroresDelDominioSeTraducenACodigosHttp() throws Exception {
        entrar("ROLE_ADMIN", null);
        doThrow(PagoConTarjetaException.pedidoYaPagado(10)).when(iniciar).iniciar(anyInt(), any(), anyInt(), any());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("El pedido 10 ya está pagado"));

        doThrow(new PagoConTarjetaException.ConflictoDeConcurrencia()).when(iniciar).iniciar(anyInt(), any(), anyInt(), any());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isConflict());

        doThrow(new PagoConTarjetaException.CobroNoCreado("Monto inválido")).when(iniciar).iniciar(anyInt(), any(), anyInt(), any());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isBadGateway());

        doThrow(new IllegalStateException("se cayo")).when(iniciar).iniciar(anyInt(), any(), anyInt(), any());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content(COBRAR_TERMINAL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void datosIncompletosOCanalInvalido_400_yElMontoDelFrontNoExiste() throws Exception {
        entrar("ROLE_ADMIN", null);
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON).content("{\"canal\":\"TERMINAL\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/pagos-tarjeta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":10,\"canal\":\"CHEQUE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void losMesesDisponiblesSoloLosVeSuDueño() throws Exception {
        entrar("ROLE_USER", 5);
        when(planes.mesesPara(10)).thenReturn(List.of(3));
        mvc.perform(get("/v1/pagos-tarjeta/pedido/10/meses")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0]").value(3));
        mvc.perform(get("/v1/pagos-tarjeta/pedido/11/meses")).andExpect(status().isNotFound());
    }
}
