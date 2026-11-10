package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;
import com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia.CampanasJpaAdapter;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El adaptador de campañas contra una base real (H2): guardar, leer, activar una sola y reemplazar la lista de precios. */
@DataJpaTest
@ActiveProfiles("test")
@Import({CampanasJpaAdapter.class, TablasDisponibles.class})
class CampanasJpaTest {

    @Autowired private CampanasJpaAdapter adaptador;
    @Autowired private IProductosRepository productosRepository;
    @Autowired private ICodigoBarrasRepository codigoBarrasRepository;
    @Autowired private EntityManager em;

    private int consecutivo;

    private Producto producto(String nombre) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("760000000" + (++consecutivo));
        cb = codigoBarrasRepository.save(cb);
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setStock(5);
        p.setHabilitado('1');
        p.setPrecioCosto(10.0);
        p.setPrecioVenta(250.0);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        return productosRepository.save(p);
    }

    private Campana nueva(String nombre, boolean activa, PrecioDeCampana... precios) {
        return new Campana(null, nombre, LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 6), activa, new BigDecimal("1500"), List.of(precios));
    }

    @Test
    void guardaLeeYReemplazaPrecios() {
        Producto carro = producto("Carro");
        Producto muneca = producto("Muñeca");
        Campana g = adaptador.guardar(nueva("Reyes", false, new PrecioDeCampana(carro.getId(), null, new BigDecimal("199.50"))));
        em.flush();
        em.clear();

        Campana leida = adaptador.buscar(g.id()).orElseThrow();
        assertThat(leida.nombre()).isEqualTo("Reyes");
        assertThat(leida.precios()).hasSize(1);
        assertThat(leida.precios().get(0).nombre()).isEqualTo("Carro");
        assertThat(leida.precios().get(0).precio()).isEqualByComparingTo("199.50");
        assertThat(leida.metaMonto()).isEqualByComparingTo("1500");

        adaptador.guardar(new Campana(g.id(), "Reyes 2027", leida.fechaInicio(), leida.fechaFin(), false, leida.metaMonto(),
                List.of(new PrecioDeCampana(muneca.getId(), null, new BigDecimal("120")), new PrecioDeCampana(carro.getId(), null, new BigDecimal("180")))));
        em.flush();
        em.clear();
        Campana cambiada = adaptador.buscar(g.id()).orElseThrow();
        assertThat(cambiada.nombre()).isEqualTo("Reyes 2027");
        assertThat(cambiada.precios()).extracting(PrecioDeCampana::productoId).containsExactlyInAnyOrder(carro.getId(), muneca.getId());
    }

    @Test
    void activarEncendidaSoloUnaYApagar() {
        Producto carro = producto("Carro");
        Campana a = adaptador.guardar(nueva("A", false, new PrecioDeCampana(carro.getId(), null, new BigDecimal("100"))));
        Campana b = adaptador.guardar(nueva("B", false, new PrecioDeCampana(carro.getId(), null, new BigDecimal("90"))));
        em.flush();
        em.clear();

        adaptador.activarSola(a.id());
        adaptador.activarSola(b.id());
        em.flush();
        em.clear();
        assertThat(adaptador.activas()).extracting(Campana::id).containsExactly(b.id());

        adaptador.apagar(b.id());
        em.flush();
        em.clear();
        assertThat(adaptador.activas()).isEmpty();

        adaptador.eliminar(a.id());
        em.flush();
        assertThat(adaptador.buscar(a.id())).isEmpty();
    }
}
