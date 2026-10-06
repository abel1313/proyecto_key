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
 * QA 2026-10-06: en el detalle del pedido, "Cambiar articulo" ofrecia articulos sin stock y
 * deshabilitados. El buscador del pedido solo debe traer lo que se puede vender ahora.
 */
@DataJpaTest
@ActiveProfiles("test")
class VarianteParaPedidoRepositoryTest {

    @Autowired private IVarianteRepository varianteRepository;
    @Autowired private IProductosRepository productosRepository;
    @Autowired private ICodigoBarrasRepository codigoBarrasRepository;

    private Producto modelo(String codigo, int stock, char habilitado) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras(codigo);
        cb = codigoBarrasRepository.save(cb);
        Producto p = new Producto();
        p.setNombre("Blusa lino");
        p.setStock(stock);
        p.setHabilitado(habilitado);
        p.setPrecioCosto(0.0);
        p.setPrecioVenta(100.0);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        return productosRepository.save(p);
    }

    private Variantes articulo(Producto p, String talla, int stock, char habilitado) {
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setTalla(talla);
        v.setStock(stock);
        v.setHabilitado(habilitado);
        return varianteRepository.save(v);
    }

    @Test
    void solo_trae_articulos_con_stock_y_habilitados_en_articulo_y_modelo() {
        Producto bueno = modelo("7500000000101", 10, '1');
        Variantes vendible = articulo(bueno, "CH", 3, '1');
        articulo(bueno, "M", 0, '1');               // sin stock
        articulo(bueno, "G", 5, '0');               // articulo dado de baja
        articulo(modelo("7500000000102", 10, '0'), "CH", 5, '1');   // modelo deshabilitado
        articulo(modelo("7500000000103", 0, '1'), "CH", 5, '1');    // modelo sin stock

        var pagina = varianteRepository.buscarVariantesParaPedido("blusa", PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(Variantes::getId).containsExactly(vendible.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void busca_por_codigo_de_barras() {
        Producto bueno = modelo("7500000000201", 10, '1');
        Variantes v = articulo(bueno, "CH", 3, '1');

        var pagina = varianteRepository.buscarVariantesParaPedido("0000201", PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(Variantes::getId).containsExactly(v.getId());
    }
}
