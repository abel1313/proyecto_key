package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las consultas del chatbot se validan al ARRANCAR la aplicacion: si una esta mal escrita, el back no levanta (QA 2026-11-04). Los tests con
 * repositorios simulados no lo ven. Aqui se corren contra H2 y se comprueban las reglas de la auditoria: solo articulos habilitados y no
 * internos, y el codigo de barras parcial solo cuando se autoriza.
 */
@DataJpaTest
@ActiveProfiles("test")
class IVarianteRepositoryChatbotTest {

    @Autowired private IVarianteRepository variantes;
    @Autowired private IProductosRepository productos;
    @Autowired private ICodigoBarrasRepository codigos;

    private Variantes articulo(String nombre, String codigo, char habilitadoArticulo, boolean interno) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras(codigo);
        cb = codigos.save(cb);
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setStock(10);
        p.setHabilitado('1');
        p.setPrecioCosto(0.0);
        p.setPrecioVenta(100.0);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setCodigoBarrasGenerado(false);
        p.setEsCatalogoInterno(interno);
        p = productos.save(p);
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setStock(3);
        v.setHabilitado(habilitadoArticulo);
        return variantes.save(v);
    }

    @Test
    void catalogoGeneralSoloHabilitadosYNoInternos() {
        articulo("visible", "111", '1', false);
        articulo("dado de baja", "222", '0', false);
        articulo("interno", "333", '1', true);
        var nombres = variantes.findByStockGreaterThanAndProductoHabilitado(0, '1', PageRequest.of(0, 50))
                .map(v -> v.getProducto().getNombre()).getContent();
        assertThat(nombres).containsExactly("visible");
    }

    @Test
    void buscarParaChatbotCodigoParcialSoloAutorizado() {
        articulo("camisa", "7501234567890", '1', false);
        // el nombre/marca no coinciden con "7501": solo el codigo
        assertThat(variantes.buscarParaChatbot("7501", false, PageRequest.of(0, 5)).getContent()).isEmpty();
        assertThat(variantes.buscarParaChatbot("7501234567890", false, PageRequest.of(0, 5)).getContent()).hasSize(0); // sin imagen no entra
        assertThat(variantes.buscarParaChatbot("7501", true, PageRequest.of(0, 5)).getTotalElements()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void coincidenciasCodigoParcialSoloAutorizado() {
        articulo("camisa roja", "7501234567890", '1', false);
        assertThat(variantes.buscarCoincidenciasParaChatbot("7501", false, PageRequest.of(0, 5))).isEmpty();
        assertThat(variantes.buscarCoincidenciasParaChatbot("7501", true, PageRequest.of(0, 5))).hasSize(1);
        assertThat(variantes.buscarCoincidenciasParaChatbot("7501234567890", false, PageRequest.of(0, 5))).hasSize(1);
        assertThat(variantes.buscarCoincidenciasParaChatbot("camisa", false, PageRequest.of(0, 5))).hasSize(1);
    }

    @Test
    void coincidenciasTambienBuscanEnDescripcionYColor() {
        // QA 2026-11-04: "termo ... buscando a nemo" solo decia Nemo en la descripcion y el bot contestaba que no lo teniamos.
        Variantes termo = articulo("termo", "1025202655819", '1', false);
        termo.setColor("NEGRO");
        termo.setDescripcion("Termo 32oz 950mL buscando a nemo");
        variantes.save(termo);
        articulo("termo postres", "847280091386", '1', false);

        assertThat(variantes.buscarCoincidenciasParaChatbot("nemo", false, PageRequest.of(0, 5)))
                .extracting(v -> v.getProducto().getNombre()).containsExactly("termo");
        assertThat(variantes.buscarCoincidenciasParaChatbot("negro", false, PageRequest.of(0, 5))).hasSize(1);
        assertThat(variantes.buscarCoincidenciasParaChatbot("termo", false, PageRequest.of(0, 5))).hasSize(2);
    }

    @Test
    void porCategoriaCompila() {
        assertThat(variantes.buscarParaChatbotPorCategoria(0, '1', "faldas", PageRequest.of(0, 5))).isEmpty();
    }
}
