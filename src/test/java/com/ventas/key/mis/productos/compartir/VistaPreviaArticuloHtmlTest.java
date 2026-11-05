package com.ventas.key.mis.productos.compartir;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VistaPreviaArticuloHtmlTest {

    @Test
    void trae_las_etiquetas_que_leen_whatsapp_y_facebook() {
        String h = VistaPreviaArticuloHtml.armar("Bolsa Aurora", "De piel", "890.00",
                "https://api.jade.mx/mis-productos/v1/imagenes/file/7", "https://shop.jade.mx/tienda/detalle/5",
                "https://api.jade.mx/mis-productos/v1/compartir/articulo/5");
        assertTrue(h.contains("property=\"og:title\" content=\"Bolsa Aurora\""));
        assertTrue(h.contains("property=\"og:image\" content=\"https://api.jade.mx/mis-productos/v1/imagenes/file/7\""));
        assertTrue(h.contains("product:price:amount\" content=\"890.00\""));
        assertTrue(h.contains("product:price:currency\" content=\"MXN\""));
        assertTrue(h.contains("http-equiv=\"refresh\" content=\"0;url=https://shop.jade.mx/tienda/detalle/5\""));
    }

    @Test
    void escapa_el_nombre_y_la_descripcion_porque_los_escribe_una_persona() {
        String h = VistaPreviaArticuloHtml.armar("<script>alert(1)</script> \"x\"", "a & b <b>", "1", "", "", "");
        assertFalse(h.contains("<script>"));
        assertTrue(h.contains("&lt;script&gt;alert(1)&lt;/script&gt; &quot;x&quot;"));
        assertTrue(h.contains("a &amp; b &lt;b&gt;"));
    }

    @Test
    void sin_foto_ni_tienda_no_pone_og_image_ni_redireccion() {
        String h = VistaPreviaArticuloHtml.armar("Falda", "", "520.00", "", "", "");
        assertFalse(h.contains("og:image"));
        assertFalse(h.contains("http-equiv=\"refresh\""));
    }

    @Test
    void una_descripcion_larga_se_recorta() {
        String h = VistaPreviaArticuloHtml.armar("X", "a".repeat(500), "1", "", "", "");
        assertTrue(h.contains("…"));
        assertFalse(h.contains("a".repeat(250)));
    }
}
