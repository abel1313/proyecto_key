package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regla del dueño (2026-11-04): si al PRODUCTO se le baja el stock y sus artículos suman más, a los artículos se les quita la diferencia;
 * si se le sube, a los artículos no se les agrega nada (lo que sobra queda libre para dar de alta artículos como siempre).
 * Ejemplo: producto de 10 repartido en 5 artículos de 2; se baja a 9 -> un artículo queda con 1.
 */
class ProductoBajaStockRecortaArticulosTest {

    private IVarianteRepository repo;
    private ProductosServiceImpl service;
    private List<Variantes> articulos;

    @BeforeEach
    void preparar() {
        repo = mock(IVarianteRepository.class);
        service = Mockito.mock(ProductosServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "varianteRepository", repo);
        // del mas nuevo (id mayor) al mas viejo, como los devuelve findByProductoIdAndHabilitadoOrderByIdDesc
        articulos = new ArrayList<>();
        for (int id = 5; id >= 1; id--) {
            Variantes v = new Variantes();
            v.setId(id);
            v.setStock(2);
            v.setHabilitado('1');
            articulos.add(v);
        }
        when(repo.findByProductoIdAndHabilitadoOrderByIdDesc(anyInt(), org.mockito.ArgumentMatchers.anyChar())).thenReturn(articulos);
    }

    private void ajustar(int nuevoStock) {
        ReflectionTestUtils.invokeMethod(service, "ajustarVariantesSiExceden", 1, nuevoStock);
    }

    private int stock(int id) {
        return articulos.stream().filter(v -> v.getId() == id).findFirst().orElseThrow().getStock();
    }

    @Test
    @DisplayName("producto 10 con 5 articulos de 2: bajarlo a 9 deja un articulo (el mas nuevo) con 1")
    void bajarUnoQuitaUnoDelMasNuevo() {
        ajustar(9);
        assertThat(stock(5)).isEqualTo(1);
        assertThat(articulos.stream().mapToInt(Variantes::getStock).sum()).isEqualTo(9);
        assertThat(stock(1)).isEqualTo(2);
    }

    @Test
    @DisplayName("bajarlo a 7 vacia el articulo mas nuevo y deja el siguiente en 1")
    void bajarVariosPasaAlSiguiente() {
        ajustar(7);
        assertThat(stock(5)).isZero();
        assertThat(stock(4)).isEqualTo(1);
        assertThat(articulos.stream().mapToInt(Variantes::getStock).sum()).isEqualTo(7);
    }

    @Test
    @DisplayName("subir el stock del producto o dejarlo igual no toca los articulos")
    void subirNoAgregaNada() {
        ajustar(15);
        ajustar(10);
        assertThat(articulos).extracting(Variantes::getStock).containsOnly(2);
    }

    @Test
    @DisplayName("bajarlo a 0 deja todos los articulos en 0, nunca negativo")
    void aCero() {
        ajustar(0);
        assertThat(articulos).extracting(Variantes::getStock).containsOnly(0);
    }
}
