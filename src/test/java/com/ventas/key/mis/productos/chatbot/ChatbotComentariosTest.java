package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Reglas de comentarios acordadas el 2026-09-24 (hexagonal/botredes/README.md, R2 y R4).
class ChatbotComentariosTest {

    private final ChatbotFacebookService facebook = new ChatbotFacebookService(null, null);

    @Test
    void sinProductoSoloAgradeceYCualquierPreguntaLaEscala() {
        String instrucciones = facebook.instruccionesSinProducto(false);

        assertThat(instrucciones).contains("NO SABES de qué producto es esta publicación");
        assertThat(instrucciones).contains("ÚNICAMENTE ##ESCALAR##");
        assertThat(instrucciones).contains("agradecimiento corto");
    }

    @Test
    void conProductoLeDaAlBotLosDatosDeEseProductoConElPrecioQueSeCobra() {
        Producto producto = new Producto();
        producto.setNombre("Bolsa Lucía");
        producto.setPrecioVenta(350.0);
        producto.setPrecioRebaja(300.0);
        Variantes variante = new Variantes();
        variante.setProducto(producto);
        variante.setColor("negro");
        variante.setStock(2);

        String instrucciones = facebook.instruccionesSobreProducto(variante, true);

        assertThat(instrucciones).contains("Bolsa Lucía", "color: negro", "$300 MXN (con descuento, antes $350)", "2 piezas");
        assertThat(instrucciones).contains("ÚNICAMENTE ##ESCALAR##");
        assertThat(instrucciones).contains("PRIMERA vez");
    }

    @Test
    void despuesDeLaPrimeraVezSaludaPeroYaNoSePresenta() {
        assertThat(facebook.instruccionesSinProducto(true)).contains("PRIMERA vez");
        assertThat(facebook.instruccionesSinProducto(false)).contains("No vuelvas a decir que eres el asistente");
    }
}
