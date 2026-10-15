package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// "Recoger en tienda" es la fila del local, no una zona (2026-10-07): solo una, y sin envio,
// horas extra ni dia de entrega.
@ExtendWith(MockitoExtension.class)
class LugarEntregaRecogerEnTiendaTest {

    @Mock private ILugarEntregaRepository repo;
    @Mock private ErrorGenerico error;
    @Mock private ProductoSombraServiceImpl productoSombraService;
    @Mock private CacheService cacheService;
    @Mock private RabbitTemplate rabbitTemplate;

    private LugarEntregaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LugarEntregaServiceImpl(repo, error, productoSombraService);
        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        ReflectionTestUtils.setField(service, "rabbitTemplate", rabbitTemplate);
    }

    private LugarEntrega lugar(Integer id, String nombre, boolean recoger) {
        LugarEntrega l = new LugarEntrega();
        l.setId(id);
        l.setNombre(nombre);
        l.setEsRecogerEnTienda(recoger);
        return l;
    }

    @Test
    void segundaFilaDeRecogerEnTienda_seRechazaYNoSeGuarda() {
        when(repo.findByEsRecogerEnTiendaTrue()).thenReturn(List.of(lugar(1, "Local Tejupilco", true)));

        assertThatThrownBy(() -> service.save(lugar(null, "El estanco", true)))
                .isInstanceOf(ExceptionErrorInesperado.class)
                .hasMessageContaining("Local Tejupilco");
        verify(repo, never()).save(any());
    }

    @Test
    void laMismaFilaDelLocal_seEdita_yPierdeEnvioHorasYDia() {
        LugarEntrega local = lugar(1, "Local Tejupilco", true);
        local.setCostoEnvio(50.0);
        local.setHorasExtraAnticipacion(24);
        local.setDiaEntregaSemanal(3);
        when(repo.findByEsRecogerEnTiendaTrue()).thenReturn(List.of(lugar(1, "Local Tejupilco", true)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LugarEntrega guardado = service.save(local);

        assertThat(guardado.getCostoEnvio()).isNull();
        assertThat(guardado.getHorasExtraAnticipacion()).isNull();
        assertThat(guardado.getDiaEntregaSemanal()).isNull();
        verifyNoInteractions(productoSombraService);
    }

    @Test
    void zonaNormal_noSeRevisaNiSeLimpia() {
        LugarEntrega zona = lugar(null, "Zacazonapan", false);
        zona.setHorasExtraAnticipacion(24);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LugarEntrega guardado = service.save(zona);

        assertThat(guardado.getHorasExtraAnticipacion()).isEqualTo(24);
        verify(repo, never()).findByEsRecogerEnTiendaTrue();
    }
}
