package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.models.PagoMPRequest;
import com.ventas.key.mis.productos.repository.IMpPaymentIntentRepository;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El camino anterior de la terminal (/v1/mp) no cobra un pedido que ya tiene un cobro nuevo en proceso. */
class MercadoPagoServiceCobroNuevoActivoTest {

    private final IMpPaymentIntentRepository viejo = mock(IMpPaymentIntentRepository.class);
    private final IPagoTarjetaIntentoRepository nuevo = mock(IPagoTarjetaIntentoRepository.class);
    private final MercadoPagoService servicio =
            new MercadoPagoService(mock(PedidoServiceImpl.class), mock(IPedidoRepository.class), viejo);

    private static PagoMPRequest solicitud(int pedidoId) {
        PagoMPRequest r = new PagoMPRequest();
        r.setPedidoId(pedidoId);
        r.setCuotas(1);
        r.setTotalMonto(450.0);
        return r;
    }

    @Test
    void conUnCobroNuevoEnProceso_noSeEnviaALaTerminalPorElCaminoAnterior() {
        ReflectionTestUtils.setField(servicio, "iPagoTarjetaIntentoRepository", nuevo);
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", true);
        when(nuevo.existsByPedidoIdAndEstadoIn(eq(10), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciarPago(solicitud(10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya tiene un cobro con tarjeta en proceso");
        verify(viejo, never()).findFirstByPedidoIdAndEstadoOrderByFechaCreacionDesc(any(), any());   // ni se tocó el flujo viejo
    }

    @Test
    void conElModuloNuevoApagado_noSeRevisaLaTablaNueva() {
        ReflectionTestUtils.setField(servicio, "iPagoTarjetaIntentoRepository", nuevo);
        ReflectionTestUtils.setField(servicio, "pagoTarjetaHabilitado", false);
        try {
            servicio.iniciarPago(solicitud(10));
        } catch (Exception ignorada) {
            // sigue al flujo viejo, que sin credenciales de prueba falla más adelante: aquí solo importa que NO preguntó a la tabla nueva
        }
        verify(nuevo, never()).existsByPedidoIdAndEstadoIn(any(), anyCollection());
    }
}
