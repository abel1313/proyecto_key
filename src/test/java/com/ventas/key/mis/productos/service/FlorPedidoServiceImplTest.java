package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesResponseDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IFraseListonPredefinidaRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Cubre fechasDisponibles() -- el calendario que le dice al cliente cuándo puede entregarse su
// ramo. Bug encontrado 2026-08-28: horaLimitePedido ("si ya pasó esta hora, hoy ya no cuenta como
// día 0 para contar los días de plazo") solo se aplicaba a la entrega URGENTE -- pedir un ramo
// normal a las 11pm contaba "hoy" igual que pedirlo a las 8am, dando una fecha de entrega que en
// la práctica no alcanzaba a cumplirse. Se unificó para que la misma hora de corte decida el día
// de arranque en los dos casos (normal y urgente).
//
// LocalTime.now() no es inyectable en el código real (no usa Clock) -- los casos "ya pasó la
// hora" / "todavía no" se prueban con cortes en los extremos del día (00:00 / 23:59) en vez de
// mockear el reloj, para que el resultado no dependa de la hora a la que corra el test.
@ExtendWith(MockitoExtension.class)
class FlorPedidoServiceImplTest {

    @Mock private ITipoFlorRepository tipoFlorRepository;
    @Mock private IColorFlorRepository colorFlorRepository;
    @Mock private ICantidadFlorValidaRepository cantidadFlorValidaRepository;
    @Mock private IAccesorioRamoRepository accesorioRamoRepository;
    @Mock private AccesorioRamoServiceImpl accesorioRamoService;
    @Mock private IFraseListonPredefinidaRepository fraseListonPredefinidaRepository;
    @Mock private ILugarEntregaRepository lugarEntregaRepository;
    @Mock private LugarEntregaAnilloServiceImpl anilloService;

    private FlorPedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FlorPedidoServiceImpl(tipoFlorRepository, colorFlorRepository,
                cantidadFlorValidaRepository, accesorioRamoRepository, accesorioRamoService,
                fraseListonPredefinidaRepository, lugarEntregaRepository, anilloService);
    }

    private CantidadFlorValida tamanoConfigurado(Integer cantidad, Integer diasNormal, LocalTime horaEntregaNormal,
                                                  Integer diasUrgente, LocalTime horaEntregaUrgente,
                                                  LocalTime horaLimitePedido, Double cargoUrgente) {
        CantidadFlorValida c = new CantidadFlorValida();
        c.setCantidad(cantidad);
        c.setDiasNormal(diasNormal);
        c.setHoraEntregaNormal(horaEntregaNormal);
        c.setDiasUrgente(diasUrgente);
        c.setHoraEntregaUrgente(horaEntregaUrgente);
        c.setHoraLimitePedido(horaLimitePedido);
        c.setCargoUrgente(cargoUrgente);
        c.setActivo(true);
        return c;
    }

    private FechasDisponiblesRequestDto request(boolean urgente) {
        FechasDisponiblesRequestDto dto = new FechasDisponiblesRequestDto();
        dto.setTipoFlorId(1);
        dto.setCantidad(12);
        dto.setUrgente(urgente);
        return dto;
    }

    @Test
    void normal_conHoraLimiteYaPasada_empiezaAContarDesdeManana() {
        // 00:00 -- "ahora" siempre es >= esto (salvo el instante exacto de medianoche), asi que
        // esto simula de forma confiable "ya paso la hora limite de hoy".
        CantidadFlorValida tam = tamanoConfigurado(12, 3, LocalTime.of(18, 0),
                null, null, LocalTime.MIDNIGHT, null);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(tam));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(false));

        LocalDate esperado = LocalDate.now().plusDays(1).plusDays(3);
        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(esperado);
    }

    @Test
    void normal_conHoraLimiteTodaviaNoPasada_empiezaAContarDesdeHoy() {
        // 23:59 -- "ahora" casi siempre es anterior a esto, simula "todavia no paso la hora limite".
        CantidadFlorValida tam = tamanoConfigurado(12, 3, LocalTime.of(18, 0),
                null, null, LocalTime.of(23, 59), null);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(tam));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(false));

        LocalDate esperado = LocalDate.now().plusDays(3);
        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(esperado);
    }

    @Test
    void normal_sinHoraLimiteConfigurada_siempreEmpiezaDesdeHoy() {
        // Catalogos viejos que nunca configuraron horaLimitePedido para la entrega normal --
        // comportamiento de antes del fix, sin cambios.
        CantidadFlorValida tam = tamanoConfigurado(12, 3, LocalTime.of(18, 0),
                null, null, null, null);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(tam));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(false));

        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void urgente_conHoraLimiteYaPasada_empiezaAContarDesdeManana() {
        // Mismo comportamiento que ya existia antes del fix -- se re-confirma que sigue igual.
        CantidadFlorValida tam = tamanoConfigurado(12, 3, LocalTime.of(18, 0),
                1, LocalTime.of(20, 0), LocalTime.MIDNIGHT, 150.0);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(tam));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(true));

        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(LocalDate.now().plusDays(1).plusDays(1));
        assertThat(resp.getCargoUrgencia()).isEqualTo(150.0);
    }

    @Test
    void urgente_conHoraLimiteTodaviaNoPasada_empiezaAContarDesdeHoy() {
        CantidadFlorValida tam = tamanoConfigurado(12, 3, LocalTime.of(18, 0),
                1, LocalTime.of(20, 0), LocalTime.of(23, 59), 150.0);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(tam));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(true));

        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void redondeaHaciaArribaAlPrimerTamanoQueAlcanceLaCantidadPedida() {
        // El pedido es de 12 flores, el tamaño de 10 no alcanza -- debe usar el de 15 (el
        // siguiente que sí cubre 12), nunca uno más chico.
        CantidadFlorValida chico  = tamanoConfigurado(10, 2, LocalTime.of(18, 0), null, null, null, null);
        CantidadFlorValida grande = tamanoConfigurado(15, 4, LocalTime.of(18, 0), null, null, null, null);
        when(cantidadFlorValidaRepository.findActivasPorTipoFlorOrdenadas(1)).thenReturn(List.of(chico, grande));

        FechasDisponiblesResponseDto resp = service.fechasDisponibles(request(false));

        assertThat(resp.getCantidadAplicada()).isEqualTo(15);
        assertThat(resp.getPrimeraFechaValida().toLocalDate()).isEqualTo(LocalDate.now().plusDays(4));
    }
}
