package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanesMsiJdbcAdapterTest {

    @Test
    void interpretaSoloPlazosClaros() {
        assertThat(PlanesMsiJdbcAdapter.interpretar("3")).isEqualTo(3);
        assertThat(PlanesMsiJdbcAdapter.interpretar(" 6 msi ")).isEqualTo(6);
        assertThat(PlanesMsiJdbcAdapter.interpretar("12 meses")).isEqualTo(12);
        assertThat(PlanesMsiJdbcAdapter.interpretar("9 MES")).isEqualTo(9);
    }

    @Test
    void textoAmbiguoNoSeInterpreta_3y6NoSon36() {
        assertThat(PlanesMsiJdbcAdapter.interpretar("3 y 6")).isNull();
        assertThat(PlanesMsiJdbcAdapter.interpretar("contado")).isNull();
        assertThat(PlanesMsiJdbcAdapter.interpretar("")).isNull();
        assertThat(PlanesMsiJdbcAdapter.interpretar(null)).isNull();
    }
}
