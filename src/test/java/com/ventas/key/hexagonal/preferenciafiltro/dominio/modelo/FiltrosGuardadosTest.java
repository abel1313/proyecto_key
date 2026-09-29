package com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiltrosGuardadosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 30, 10, 0);

    @Test
    @DisplayName("un objeto JSON se guarda sin los espacios de las orillas")
    void objetoValido() {
        FiltrosGuardados f = new FiltrosGuardados(5, Pantalla.TIENDA_BUSCAR, "  {\"filtroTalla\":\"M\"} ", AHORA);

        assertThat(f.filtrosJson()).isEqualTo("{\"filtroTalla\":\"M\"}");
    }

    @Test
    @DisplayName("R5: lo que no es un objeto JSON se rechaza")
    void noEsObjeto() {
        assertThatThrownBy(() -> new FiltrosGuardados(5, Pantalla.TIENDA_BUSCAR, "[1,2]", AHORA))
                .isInstanceOf(PreferenciaFiltroException.class)
                .hasMessageContaining("objeto JSON");
        assertThatThrownBy(() -> new FiltrosGuardados(5, Pantalla.TIENDA_BUSCAR, null, AHORA))
                .isInstanceOf(PreferenciaFiltroException.class);
    }

    @Test
    @DisplayName("R5: más de 2000 caracteres se rechaza")
    void demasiadoGrandes() {
        String grande = "{\"x\":\"" + "a".repeat(FiltrosGuardados.MAX_CARACTERES) + "\"}";

        assertThatThrownBy(() -> new FiltrosGuardados(5, Pantalla.PRODUCTOS_BUSCAR, grande, AHORA))
                .isInstanceOf(PreferenciaFiltroException.class)
                .hasMessageContaining("máximo es 2000");
    }

    @Test
    @DisplayName("R7: {} y vacío cuentan como 'Limpiar'; un filtro con algo adentro no")
    void vacios() {
        assertThat(FiltrosGuardados.estanVacios("{}")).isTrue();
        assertThat(FiltrosGuardados.estanVacios(" { } ")).isTrue();
        assertThat(FiltrosGuardados.estanVacios("")).isTrue();
        assertThat(FiltrosGuardados.estanVacios(null)).isTrue();
        assertThat(FiltrosGuardados.estanVacios("{\"mostrarConStock\":false}")).isFalse();
    }

    @Test
    @DisplayName("R2: solo tienda-buscar y productos-buscar")
    void pantallas() {
        assertThat(Pantalla.deClave("tienda-buscar")).isEqualTo(Pantalla.TIENDA_BUSCAR);
        assertThat(Pantalla.deClave("productos-buscar").ruta()).isEqualTo("productos/buscar");
        assertThatThrownBy(() -> Pantalla.deClave("pedidos"))
                .isInstanceOf(PreferenciaFiltroException.class)
                .hasMessageContaining("no guarda filtros");
    }
}
