package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Las reglas que decide el pedido por si mismo: si esta abierto, que lineas tiene y cuanto suma. */
class PedidoEditableTest {

    private static ArticuloDePedido linea(int detalleId, int varianteId, int cantidad,
                                          double precio, Integer promocionId) {
        return new ArticuloDePedido(detalleId, varianteId, 99, "Articulo " + varianteId,
                cantidad, precio, promocionId);
    }

    private static PedidoEditable pedido(String estado, ArticuloDePedido... lineas) {
        return new PedidoEditable(1, estado, 0.0, List.of(lineas));
    }

    @Test
    @DisplayName("un pedido pendiente esta abierto")
    void pendienteEstaAbierto() {
        assertThat(pedido("Pendiente", linea(1, 10, 1, 100, null)).estaCerrado()).isFalse();
    }

    @Test
    @DisplayName("entregado y cancelado estan cerrados, y dicen por que")
    void cerradosDicenPorQue() {
        assertThat(pedido("Entregado").estaCerrado()).isTrue();
        assertThat(pedido("Entregado").motivoDelCierre()).isEqualTo("ya se entrego");
        assertThat(pedido("cancelado").estaCerrado()).isTrue();
        assertThat(pedido("cancelado").motivoDelCierre()).isEqualTo("esta cancelado");
    }

    @Test
    @DisplayName("el total se suma de las lineas, no se guarda")
    void elTotalSeSuma() {
        PedidoEditable p = pedido("Pendiente",
                linea(1, 10, 2, 100, null),
                linea(2, 11, 1, 50, null));

        assertThat(p.total()).isEqualTo(250.0);
    }

    @Test
    @DisplayName("una linea de promocion no absorbe lo que se agrega (R5)")
    void laLineaDePromocionNoAbsorbe() {
        // Misma variante 10: una en promocion y nada normal. Agregar la 10 tiene que crear una
        // linea nueva a precio normal, no engordar la promocional al precio del combo.
        PedidoEditable p = pedido("Pendiente", linea(1, 10, 1, 80, 7));

        assertThat(p.lineaNormalDe(10)).isEmpty();
    }

    @Test
    @DisplayName("si ya hay linea normal de esa variante, se encuentra (R6)")
    void laLineaNormalSeEncuentra() {
        PedidoEditable p = pedido("Pendiente",
                linea(1, 10, 1, 80, 7),
                linea(2, 10, 3, 100, null));

        assertThat(p.lineaNormalDe(10)).isPresent();
        assertThat(p.lineaNormalDe(10).get().detalleId()).isEqualTo(2);
    }

    @Test
    @DisplayName("las lineas de una promocion salen todas juntas (R4)")
    void elComboSaleCompleto() {
        PedidoEditable p = pedido("Pendiente",
                linea(1, 10, 1, 80, 7),
                linea(2, 11, 1, 90, 7),
                linea(3, 12, 1, 200, null));

        assertThat(p.lineasDe(7)).hasSize(2).allMatch(ArticuloDePedido::esDePromocion);
    }

    @Test
    @DisplayName("quitar el combo entero de un pedido que solo tiene eso lo dejaria vacio")
    void quedariaVacio() {
        PedidoEditable p = pedido("Pendiente",
                linea(1, 10, 1, 80, 7),
                linea(2, 11, 1, 90, 7));

        assertThat(p.quedariaVacio(p.lineasDe(7))).isTrue();
    }

    @Test
    @DisplayName("si queda algo mas, quitar el combo no lo deja vacio")
    void noQuedariaVacio() {
        PedidoEditable p = pedido("Pendiente",
                linea(1, 10, 1, 80, 7),
                linea(2, 12, 1, 200, null));

        assertThat(p.quedariaVacio(p.lineasDe(7))).isFalse();
    }

    @Test
    @DisplayName("el subtotal se calcula, nunca se recibe")
    void elSubtotalSeCalcula() {
        assertThat(linea(1, 10, 3, 99.5, null).subTotal()).isEqualTo(298.5);
    }
}
