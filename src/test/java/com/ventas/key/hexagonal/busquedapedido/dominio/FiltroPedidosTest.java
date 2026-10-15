package com.ventas.key.hexagonal.busquedapedido.dominio;

import com.ventas.key.hexagonal.busquedapedido.dominio.excepcion.FiltroInvalidoException;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.OrdenDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.RangoDeFechas;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.TextoBuscado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas del filtro que no necesitan base (README de busquedapedido). */
class FiltroPedidosTest {

    private static FiltroPedidos filtro(TextoBuscado texto, Double min, Double max, int pagina, int tamano) {
        return new FiltroPedidos(texto, null, null, null, min, max, null, null, null, null, null,
                false, false, null, pagina, tamano);
    }

    @Test
    @DisplayName("R1: vacio no filtra; con letras pide 3; un numero vale desde 1 digito")
    void texto() {
        assertThat(TextoBuscado.de(null)).isNull();
        assertThat(TextoBuscado.de("   ")).isNull();

        assertThatThrownBy(() -> TextoBuscado.de("ma"))
                .isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("al menos 3 letras");
        assertThat(TextoBuscado.de(" Mar ").valor()).isEqualTo("Mar");

        TextoBuscado siete = TextoBuscado.de("7");
        assertThat(siete.esNumero()).isTrue();
        assertThat(siete.comoNumeroDePedido()).isEqualTo(7);
        assertThat(siete.sirveParaTelefono()).isFalse();

        assertThat(TextoBuscado.de("#120").comoNumeroDePedido()).isEqualTo(120);
        assertThat(TextoBuscado.de("5512345678").comoNumeroDePedido()).isNull(); // telefono, no cabe como pedido
        assertThat(TextoBuscado.de("5512345678").sirveParaTelefono()).isTrue();
    }

    @Test
    @DisplayName("R1: maximo 100 caracteres")
    void textoLargo() {
        assertThatThrownBy(() -> TextoBuscado.de("x".repeat(101))).isInstanceOf(FiltroInvalidoException.class);
    }

    @Test
    @DisplayName("R5: total no negativo y desde <= hasta")
    void total() {
        assertThatThrownBy(() -> filtro(null, -1.0, null, 0, 10)).hasMessageContaining("negativo");
        assertThatThrownBy(() -> filtro(null, 500.0, 100.0, 0, 10)).hasMessageContaining("mayor que");
        assertThat(filtro(null, 100.0, 100.0, 0, 10).totalMinimo()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("R6: desde no va despues de hasta; sin fechas no hay rango")
    void fechas() {
        LocalDate uno = LocalDate.of(2026, 10, 1);
        LocalDate dos = LocalDate.of(2026, 10, 2);
        assertThatThrownBy(() -> new RangoDeFechas(dos, uno)).hasMessageContaining("va despues");
        assertThat(RangoDeFechas.de(null, null)).isNull();
        assertThat(RangoDeFechas.de(uno, null).hasta()).isNull();
    }

    @Test
    @DisplayName("R7: hoy, mañana, la semana (7 dias) y atrasados (antes de hoy)")
    void entrega() {
        LocalDate hoy = LocalDate.of(2026, 10, 6);
        assertThat(CuandoSeEntrega.HOY.rango(hoy)).isEqualTo(new RangoDeFechas(hoy, hoy));
        assertThat(CuandoSeEntrega.MANANA.rango(hoy).desde()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(CuandoSeEntrega.ESTA_SEMANA.rango(hoy).hasta()).isEqualTo(LocalDate.of(2026, 10, 12));
        RangoDeFechas atrasados = CuandoSeEntrega.ATRASADOS.rango(hoy);
        assertThat(atrasados.desde()).isNull();
        assertThat(atrasados.hasta()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    @DisplayName("R11 y R12: orden por defecto, pagina desde 0 y de 1 a 50 por pagina")
    void paginas() {
        FiltroPedidos f = filtro(null, null, null, 0, 10);
        assertThat(f.orden()).isEqualTo(OrdenDePedidos.RECIENTES);
        assertThat(f.formas()).isEmpty();
        assertThatThrownBy(() -> filtro(null, null, null, -1, 10)).hasMessageContaining("pagina");
        assertThatThrownBy(() -> filtro(null, null, null, 0, 0)).hasMessageContaining("de 1 a 50");
        assertThatThrownBy(() -> filtro(null, null, null, 0, 51)).hasMessageContaining("de 1 a 50");
    }

    @Test
    @DisplayName("R9: el numero exacto sale del texto")
    void numeroExacto() {
        assertThat(filtro(TextoBuscado.de("120"), null, null, 0, 10).numeroExacto()).isEqualTo(120);
        assertThat(filtro(TextoBuscado.de("maria"), null, null, 0, 10).numeroExacto()).isNull();
        assertThat(filtro(null, null, null, 0, 10).numeroExacto()).isNull();
    }

    @Test
    @DisplayName("R12: total de paginas")
    void totalPaginas() {
        assertThat(new PaginaDePedidos(List.of(1, 2), 21, 0, 10).totalPaginas()).isEqualTo(3);
        assertThat(new PaginaDePedidos(List.of(), 0, 0, 10).totalPaginas()).isZero();
    }
}
