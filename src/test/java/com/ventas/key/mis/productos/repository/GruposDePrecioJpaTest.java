package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.AdministrarGruposService;
import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.GruposDePrecioJpaAdapter;
import com.ventas.key.mis.productos.entity.Producto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los grupos de precio por cantidad contra una base real (H2): guardar, leer, moverlos de grupo, apagar y borrar. */
@DataJpaTest
@ActiveProfiles("test")
@Import(GruposDePrecioJpaAdapter.class)
class GruposDePrecioJpaTest {

    @Autowired private GruposDePrecioJpaAdapter adaptador;
    @Autowired private EntityManager em;

    private int producto(String nombre) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecioCosto(50.0);
        p.setPrecioVenta(250.0);
        p.setStock(10);
        p.setHabilitado('1');
        em.persist(p);
        em.flush();
        return p.getId();
    }

    private static GrupoPrecioCantidad grupo(Integer id, String nombre, int minimo, boolean activo, MiembroDeGrupo... m) {
        return new GrupoPrecioCantidad(id, nombre, minimo, null, null, activo, List.of(m));
    }

    @Test
    void guardaYLee_conSusProductosYPrecios() {
        int a = producto("Bolsa A"), b = producto("Bolsa B");
        var guardado = adaptador.guardar(grupo(null, "Bolsas navidad", 3, true,
                new MiembroDeGrupo(a, null, new BigDecimal("200")), new MiembroDeGrupo(b, null, new BigDecimal("270.5"))));
        em.clear();

        var leido = adaptador.buscar(guardado.id()).orElseThrow();
        assertThat(leido.nombre()).isEqualTo("Bolsas navidad");
        assertThat(leido.minimoPiezas()).isEqualTo(3);
        assertThat(leido.miembros()).extracting(MiembroDeGrupo::productoId).containsExactlyInAnyOrder(a, b);
        assertThat(leido.miembro(b).orElseThrow().precioPromocion()).isEqualByComparingTo("270.50");
        assertThat(leido.miembro(a).orElseThrow().nombre()).isEqualTo("Bolsa A");
        assertThat(adaptador.listar()).hasSize(1);
        assertThat(adaptador.grupoDelProducto(a)).get().extracting(GrupoPrecioCantidad::nombre).isEqualTo("Bolsas navidad");
    }

    @Test
    void alActualizar_sePuedenQuitarYAgregarProductos_yUnoQuitadoQuedaLibre() {
        int a = producto("A"), b = producto("B"), c = producto("C");
        var g = adaptador.guardar(grupo(null, "G", 2, true, new MiembroDeGrupo(a, null, new BigDecimal("100")), new MiembroDeGrupo(b, null, new BigDecimal("100"))));
        em.clear();
        adaptador.guardar(grupo(g.id(), "G", 2, true, new MiembroDeGrupo(b, null, new BigDecimal("90")), new MiembroDeGrupo(c, null, new BigDecimal("80"))));
        em.clear();

        var leido = adaptador.buscar(g.id()).orElseThrow();
        assertThat(leido.miembros()).extracting(MiembroDeGrupo::productoId).containsExactlyInAnyOrder(b, c);
        assertThat(leido.miembro(b).orElseThrow().precioPromocion()).isEqualByComparingTo("90");
        assertThat(adaptador.grupoDelProducto(a)).isEmpty();
    }

    @Test
    void soloLosActivosQueIncluyenAlgunProductoDeLaVenta() {
        int a = producto("A"), b = producto("B"), c = producto("C");
        adaptador.guardar(grupo(null, "Activo", 2, true, new MiembroDeGrupo(a, null, new BigDecimal("100"))));
        adaptador.guardar(grupo(null, "Apagado", 2, false, new MiembroDeGrupo(b, null, new BigDecimal("100"))));
        em.clear();

        assertThat(adaptador.activosConProductos(Set.of(a, b, c))).extracting(GrupoPrecioCantidad::nombre).containsExactly("Activo");
        assertThat(adaptador.activosConProductos(Set.of(c))).isEmpty();
        assertThat(adaptador.activosConProductos(Set.of())).isEmpty();
    }

    @Test
    void unProductoEnDosGrupos_lorechazaElCasoDeUso_conElNombreDelGrupo() {
        int a = producto("Bolsa A");
        var admin = new AdministrarGruposService(adaptador);
        admin.crear(grupo(null, "Primero", 2, true, new MiembroDeGrupo(a, "Bolsa A", new BigDecimal("100"))));
        em.clear();
        assertThatThrownBy(() -> admin.crear(grupo(null, "Segundo", 2, true, new MiembroDeGrupo(a, "Bolsa A", new BigDecimal("90")))))
                .isInstanceOf(GrupoDePrecioInvalidoException.class).hasMessageContaining("«Primero»");
    }

    @Test
    void productoInexistente_seAvisa_yApagarYBorrarFuncionan() {
        int a = producto("A");
        assertThatThrownBy(() -> adaptador.guardar(grupo(null, "G", 2, true, new MiembroDeGrupo(999999, null, new BigDecimal("10")))))
                .hasMessageContaining("ya no existe");
        var g = adaptador.guardar(grupo(null, "G", 2, true, new MiembroDeGrupo(a, null, new BigDecimal("10"))));
        adaptador.cambiarActivo(g.id(), false);
        em.flush();   // la prueba corre dentro de UNA transaccion: sin flush, el clear descarta el cambio
        em.clear();
        assertThat(adaptador.buscar(g.id()).orElseThrow().activo()).isFalse();
        adaptador.eliminar(g.id());
        em.flush();
        em.clear();
        assertThat(adaptador.buscar(g.id())).isEmpty();
        assertThat(adaptador.grupoDelProducto(a)).isEmpty();
    }
}
