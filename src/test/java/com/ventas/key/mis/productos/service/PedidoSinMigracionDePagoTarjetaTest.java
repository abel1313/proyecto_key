package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresion del revisor: con el modulo apagado (produccion sin migration_pago_tarjeta.sql) cancelar un pedido o cambiar su
 * forma de cobro NO debe consultar la tabla de cobros con tarjeta; con el modulo encendido y un cobro en proceso, si bloquea.
 */
class PedidoSinMigracionDePagoTarjetaTest {

    private static void revisar(PedidoServiceImpl s, int pedidoId, String queSeIntenta) throws Throwable {
        Method m = PedidoServiceImpl.class.getDeclaredMethod("exigirSinCobroConTarjeta", int.class, String.class);
        m.setAccessible(true);
        try {
            m.invoke(s, pedidoId, queSeIntenta);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private PedidoServiceImpl servicio(boolean habilitado, IPagoTarjetaIntentoRepository repo) {
        PedidoServiceImpl s = new PedidoServiceImpl(mock(IPedidoRepository.class), null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(s, "iPagoTarjetaIntentoRepository", repo);
        ReflectionTestUtils.setField(s, "pagoTarjetaHabilitado", habilitado);
        return s;
    }

    @Test
    void conElModuloApagadoNoSeConsultaLaTabla_aunqueNoExista() {
        IPagoTarjetaIntentoRepository repo = mock(IPagoTarjetaIntentoRepository.class);
        when(repo.existsByPedidoIdAndEstadoIn(anyInt(), any(Collection.class)))
                .thenThrow(new org.springframework.dao.InvalidDataAccessResourceUsageException("Table not found"));
        assertThatCode(() -> revisar(servicio(false, repo), 5, "cancelar")).doesNotThrowAnyException();
        verify(repo, never()).existsByPedidoIdAndEstadoIn(anyInt(), any(Collection.class));
    }

    @Test
    void conElModuloEncendidoUnCobroEnProcesoBloquea() {
        IPagoTarjetaIntentoRepository repo = mock(IPagoTarjetaIntentoRepository.class);
        when(repo.existsByPedidoIdAndEstadoIn(anyInt(), any(Collection.class))).thenReturn(true);
        assertThatThrownBy(() -> revisar(servicio(true, repo), 5, "cancelar"))
                .hasMessageContaining("cobro con tarjeta").hasMessageContaining("cancelar");
    }

    @Test
    void conElModuloEncendidoSinCobrosNoBloquea() {
        IPagoTarjetaIntentoRepository repo = mock(IPagoTarjetaIntentoRepository.class);
        assertThatCode(() -> revisar(servicio(true, repo), 5, "cancelar")).doesNotThrowAnyException();
    }
}
