package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Regresion del incidente de produccion 2026-09-09: un borrador de la carga rapida desaparecio de
// la pantalla de Carga rapida (que lo listaba por codigoBarrasGenerado=true + habilitado=false)
// pero siguio saliendo en productos/buscar, y desde ahi no habia forma de completarlo. Estos
// tests fijan las dos mitades del contrato: un borrador se reconoce por el flag O por el codigo
// placeholder BRD- (sin mirar habilitado), y no se filtra a ningun listado de admin.
@DataJpaTest
@ActiveProfiles("test")
class IProductosRepositoryBorradorTest {

    @Autowired private IProductosRepository productosRepository;
    @Autowired private ICodigoBarrasRepository codigoBarrasRepository;

    private Producto guardar(String codigo, Boolean codigoBarrasGenerado, char habilitado) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras(codigo);
        cb = codigoBarrasRepository.save(cb);

        Producto producto = new Producto();
        producto.setNombre("producto " + codigo);
        producto.setStock(1);
        producto.setHabilitado(habilitado);
        producto.setPrecioCosto(0.0);
        producto.setPrecioVenta(0.0);
        producto.setPrecioRebaja(0.0);
        producto.setPiezas(0.0);
        producto.setCodigoBarras(cb);
        producto.setCodigoBarrasGenerado(codigoBarrasGenerado);
        producto.setEsCatalogoInterno(false);
        return productosRepository.save(producto);
    }

    private List<Integer> idsFiltroAdmin(Boolean codigoGenerado) {
        return productosRepository
                .buscarProductosAdmin(null, null, null, null, codigoGenerado, null, null, PageRequest.of(0, 50))
                .getContent().stream().map(Producto::getId).toList();
    }

    @Test
    void findBorradores_encuentra_el_borrador_aunque_el_flag_se_haya_perdido() {
        // El caso exacto que rompio produccion: codigo todavia BRD-, flag en false.
        Producto drift = guardar("BRD-ABC123456789", false, '0');
        Producto normal = guardar("7501234567890", false, '1');

        List<Integer> ids = productosRepository.findBorradores().stream().map(Producto::getId).toList();

        assertThat(ids).contains(drift.getId()).doesNotContain(normal.getId());
    }

    @Test
    void findBorradores_no_depende_de_habilitado() {
        // Guardar desde productos/add ponia habilitado='1' y el borrador se caia del listado.
        Producto habilitadoPorError = guardar("BRD-DEF123456789", true, '1');

        assertThat(productosRepository.findBorradores())
                .extracting(Producto::getId).contains(habilitadoPorError.getId());
    }

    @Test
    void los_borradores_no_se_propagan_a_los_listados_de_admin() {
        Producto borrador = guardar("BRD-GHI123456789", true, '0');
        Producto driftFlag = guardar("BRD-JKL123456789", false, '0');
        Producto real = guardar("7509876543210", false, '1');

        List<Integer> visibleParaAdmin = productosRepository.findVisibleParaAdmin(PageRequest.of(0, 50))
                .getContent().stream().map(Producto::getId).toList();
        assertThat(visibleParaAdmin).contains(real.getId())
                .doesNotContain(borrador.getId(), driftFlag.getId());

        // codigoGenerado sin valor = productos/buscar y tienda/buscar: tampoco deben verse ahi.
        assertThat(idsFiltroAdmin(null)).contains(real.getId())
                .doesNotContain(borrador.getId(), driftFlag.getId());
        assertThat(idsFiltroAdmin(false)).contains(real.getId())
                .doesNotContain(borrador.getId(), driftFlag.getId());
    }

    @Test
    void el_filtro_con_codigoGenerado_true_sigue_trayendo_los_borradores() {
        Producto borrador = guardar("BRD-MNO123456789", true, '0');
        Producto driftFlag = guardar("BRD-PQR123456789", false, '0');
        Producto real = guardar("7501111111111", false, '1');

        assertThat(idsFiltroAdmin(true))
                .contains(borrador.getId(), driftFlag.getId())
                .doesNotContain(real.getId());
    }
}
