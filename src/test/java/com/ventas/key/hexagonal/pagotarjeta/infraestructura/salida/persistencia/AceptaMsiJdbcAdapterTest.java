package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Con mocks: lo importante es que SIN la columna (migración sin correr) el error sea claro y no un 500 técnico. */
class AceptaMsiJdbcAdapterTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AceptaMsiJdbcAdapter adaptador = new AceptaMsiJdbcAdapter(jdbc);

    private static BadSqlGrammarException sinColumna() {
        return new BadSqlGrammarException("x", "SELECT", new SQLSyntaxErrorException("Unknown column 'acepta_msi'"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinLaColumnaLeerElProductoDaUnMensajeClaro() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenThrow(sinColumna());
        assertThatThrownBy(() -> adaptador.productoAcepta(1))
                .isInstanceOf(PagoConTarjetaException.CasillaMsiNoDisponible.class)
                .hasMessageContaining("migration_pago_tarjeta.sql");
    }

    @Test
    void sinLaColumnaGuardarTambienDaUnMensajeClaro() {
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(sinColumna());
        assertThatThrownBy(() -> adaptador.fijarProducto(1, true))
                .isInstanceOf(PagoConTarjetaException.CasillaMsiNoDisponible.class);
        assertThatThrownBy(() -> adaptador.fijarArticulo(1, null))
                .isInstanceOf(PagoConTarjetaException.CasillaMsiNoDisponible.class);
    }

    @Test
    void sinLaColumnaConsultarVariosTambien_peroSinIdsNoTocaLaBase() {
        assertThat(adaptador.efectivos(List.of())).isEmpty();
        assertThat(adaptador.efectivos(null)).isEmpty();
    }

    @Test
    void siMysqlDiceCeroFilasPorqueElValorYaEraElMismo_noLoTomaPorInexistente() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbc.queryForList(anyString(), org.mockito.ArgumentMatchers.<Class<Integer>>any(), anyInt())).thenReturn(List.of(5));
        assertThat(adaptador.fijarProducto(5, true)).isTrue();
    }

    @Test
    void siDeVerasNoExiste_devuelveFalse() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbc.queryForList(anyString(), org.mockito.ArgumentMatchers.<Class<Integer>>any(), anyInt())).thenReturn(List.of());
        assertThat(adaptador.fijarProducto(404, true)).isFalse();
        assertThat(adaptador.fijarArticulo(404, true)).isFalse();
    }
}
