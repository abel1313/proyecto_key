package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Valida las dos reglas de stock que se rompieron en produccion el 2026-09-22:
 * una variante dada de baja no debe retener stock, y editar una variante sin cambiar su stock
 * no debe consumir stock.
 *
 * <p>Se llama al metodo privado por reflexion a proposito: la alternativa es pasar por
 * {@code guardarConImagenes()}, que ademas sube imagenes al microservicio y persiste -- eso
 * probaria muchas cosas a la vez y ninguna bien.
 */
class VarianteStockValidacionTest {

    private IVarianteRepository varianteRepo;
    private IProductosRepository productoRepo;
    private VarianteServiceImpl service;

    private static final int PRODUCTO_ID = 279;

    @BeforeEach
    void setUp() {
        varianteRepo = mock(IVarianteRepository.class);
        productoRepo = mock(IProductosRepository.class);

        service = new VarianteServiceImpl(
                varianteRepo,
                mock(IVarianteImagenRepository.class),
                productoRepo,
                mock(IProductoImagenRepository.class),
                mock(ImageneClienteDisco.class),
                mock(IImagenRepository.class),
                mock(ImagenPort.class),
                mock(IPalabraClaveRepository.class),
                mock(ICodigoBarrasRepository.class),
                mock(ErrorGenerico.class));
    }

    private void validar(List<VarianteDetalle> detalles) {
        ReflectionTestUtils.invokeMethod(service, "validarStockContraProducto", detalles);
    }

    private static Producto producto(int stock) {
        Producto p = new Producto();
        p.setId(PRODUCTO_ID);
        p.setNombre("Great Jeans");
        p.setStock(stock);
        return p;
    }

    private static Variantes variante(int id, int stock, char habilitado) {
        Variantes v = new Variantes();
        v.setId(id);
        v.setStock(stock);
        v.setHabilitado(habilitado);
        return v;
    }

    private static VarianteDetalle detalle(Integer id, int stock) {
        VarianteDetalle d = new VarianteDetalle();
        d.setId(id);
        d.setProductoId(PRODUCTO_ID);
        d.setStock(stock);
        return d;
    }

    private void productoTiene(int stock) {
        when(productoRepo.findById(PRODUCTO_ID)).thenReturn(Optional.of(producto(stock)));
    }

    @Test
    @DisplayName("editar una variante sin cambiar su stock no consume stock aunque no quede nada libre")
    void editarSinCambiarStockNoConsume() {
        // El caso exacto de produccion: el admin solo queria cambiar el nombre. El front reenvia
        // el stock actual (1), y antes eso se contaba como si pidiera 1 nuevo.
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 1, '1'), variante(2, 9, '1')));
        when(varianteRepo.findAllById(anyIterable()))
                .thenReturn(List.of(variante(1, 1, '1')));

        assertThatCode(() -> validar(List.of(detalle(1, 1)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bajarle stock a una variante nunca falla: libera, no pide")
    void bajarStockNoFalla() {
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 5, '1'), variante(2, 5, '1')));
        when(varianteRepo.findAllById(anyIterable()))
                .thenReturn(List.of(variante(1, 5, '1')));

        assertThatCode(() -> validar(List.of(detalle(1, 2)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una variante dada de baja no retiene stock: el disponible la ignora")
    void varianteDeBajaNoRetieneStock() {
        // Producto con 10. Una variante habilitada usa 2 y una dada de baja "tenia" 8.
        // Antes: disponible = 10 - (2+8) = 0 -> no se podia hacer nada.
        // Ahora: disponible = 10 - 2 = 8.
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 2, '1'), variante(2, 8, '0')));
        when(varianteRepo.findAllById(anyIterable())).thenReturn(List.of());

        assertThatCode(() -> validar(List.of(detalle(null, 8)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pedir mas de lo disponible sigue fallando, con el numero correcto")
    void pedirDeMasFalla() {
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 8, '1')));
        when(varianteRepo.findAllById(anyIterable())).thenReturn(List.of());

        assertThatThrownBy(() -> validar(List.of(detalle(null, 3))))
                .isInstanceOf(ExceptionDataNotFound.class)
                .hasMessageContaining("Disponible: 2")
                .hasMessageContaining("Solicitado: 3");
    }

    @Test
    @DisplayName("subirle stock a una variante existente solo pide la diferencia")
    void subirStockPideSoloElDelta() {
        // Variante con 3 pasa a 5: pide 2, no 5. Disponible = 10 - 4 (la otra) = 6.
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 3, '1'), variante(2, 4, '1')));
        when(varianteRepo.findAllById(anyIterable()))
                .thenReturn(List.of(variante(1, 3, '1')));

        assertThatCode(() -> validar(List.of(detalle(1, 5)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("en un mismo guardado, lo que una variante libera lo puede tomar otra")
    void loQueUnaLiberaOtraLoToma() {
        // Sin stock libre: una baja de 5 a 2 y la otra sube de 5 a 8. Neto 0, tiene que pasar.
        productoTiene(10);
        when(varianteRepo.findByProductoId(PRODUCTO_ID))
                .thenReturn(List.of(variante(1, 5, '1'), variante(2, 5, '1')));
        when(varianteRepo.findAllById(anyIterable()))
                .thenReturn(List.of(variante(1, 5, '1'), variante(2, 5, '1')));

        assertThatCode(() -> validar(List.of(detalle(1, 2), detalle(2, 8))))
                .doesNotThrowAnyException();
    }
}
