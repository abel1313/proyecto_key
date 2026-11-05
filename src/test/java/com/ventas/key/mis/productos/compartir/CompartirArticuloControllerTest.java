package com.ventas.key.mis.productos.compartir;

import com.ventas.key.mis.productos.compartir.CompartirArticuloController;
import com.ventas.key.mis.productos.compartir.VistaPreviaArticuloHtml;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.VarianteServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CompartirArticuloControllerTest {
    IVarianteRepository repo = mock(IVarianteRepository.class);
    VarianteServiceImpl serv = mock(VarianteServiceImpl.class);

    Variantes articulo(char hv, char hp, Boolean interno, Double precio, String desc) {
        Producto p = new Producto(); p.setNombre("Bolsa \"Aurora\" <b>"); p.setHabilitado(hp); p.setEsCatalogoInterno(interno);
        p.setPrecioVenta(precio); p.setPrecioRebaja(1.0); p.setPrecioCosto(3.0); p.setStock(77); p.setDescripcion(null);
        Variantes v = new Variantes(); v.setId(5); v.setProducto(p); v.setHabilitado(hv); v.setDescripcion(desc); v.setStock(41);
        return v;
    }

    CompartirArticuloController ctl(String back, String tienda) { return new CompartirArticuloController(repo, serv, back, tienda); }

    @Test void ids_raros_dan_404_html_sin_tocar_bd() {
        for (String id : new String[]{"-1", "0", "abc", "99999999999", "1e3", ""}) {
            var r = ctl("https://b.mx/mis-productos", "https://t.mx").articulo(id);
            assertThat(r.getStatusCode().value()).as(id).isEqualTo(404);
        }
        verifyNoInteractions(repo);
    }

    @Test void producto_sombra_y_deshabilitados_dan_404() {
        when(repo.findById(5)).thenReturn(Optional.of(articulo('1', '1', true, 20.0, null)));
        assertThat(ctl("", "").articulo("5").getStatusCode().value()).isEqualTo(404);
        when(repo.findById(5)).thenReturn(Optional.of(articulo('0', '1', false, 20.0, null)));
        assertThat(ctl("", "").articulo("5").getStatusCode().value()).isEqualTo(404);
    }

    @Test void nulos_no_truenan_y_no_salen_datos_internos() {
        when(repo.findById(5)).thenReturn(Optional.of(articulo('1', '1', null, null, null)));
        when(serv.getImagenesPorVarianteV2(anyInt())).thenThrow(new RuntimeException("redis caido"));
        var r = ctl("https://b.mx/mis-productos/", "javascript:alert(1)").articulo("5");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        String h = r.getBody();
        assertThat(h).doesNotContain("javascript").doesNotContain("refresh").doesNotContain("price:amount")
                .doesNotContain("77").doesNotContain("41").doesNotContain("3.00").doesNotContain("1.00")
                .contains("Bolsa &quot;Aurora&quot; &lt;b&gt;");
    }

    @Test void recorte_no_parte_emojis() {
        String h = VistaPreviaArticuloHtml.armar("X", "a".repeat(198) + "😀😀😀", "1.00", "", "", "");
        assertThat(h).doesNotContain("�");
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if (Character.isHighSurrogate(c)) assertThat(Character.isLowSurrogate(h.charAt(i + 1))).isTrue();
        }
    }
}
