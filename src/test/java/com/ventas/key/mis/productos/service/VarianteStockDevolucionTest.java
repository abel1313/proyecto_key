package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre la regla de stock acordada: el stock base del producto es el total fisico y solo se mueve
 * desde la pantalla del producto (mercancia que llega o se vende). Las variantes se reparten ese
 * total, asi que dar de baja o deshabilitar una variante la deja en 0 -- con eso su stock vuelve a
 * estar disponible para otras -- pero NO sube el stock base.
 *
 * Base 10 con una variante en 2: disponible 8. Al darla de baja, base sigue 10 y disponible 10.
 *
 * Antes de este cambio los flujos de baja/deshabilitado dejaban el stock colgado en la variante, y
 * validarStockContraProducto() lo seguia contando como "usado" aunque ya no se vendiera.
 */
@ExtendWith(MockitoExtension.class)
class VarianteStockDevolucionTest {

    @Mock private IVarianteRepository iVarianteRepository;
    @Mock private IVarianteImagenRepository iVarianteImagenRepository;
    @Mock private IProductosRepository iProductosRepository;
    @Mock private IProductoImagenRepository iProductoImagenRepository;
    @Mock private ImageneClienteDisco imageneClienteDisco;
    @Mock private IImagenRepository iImagenRepository;
    @Mock private ImagenPort imagenPort;
    @Mock private IPalabraClaveRepository iPalabraClaveRepository;
    @Mock private ICodigoBarrasRepository iCodigoBarrasRepository;
    @Mock private ErrorGenerico errorGenerico;
    @Mock private CacheManager cacheManager;
    @Mock private EntityManager entityManager;

    private VarianteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VarianteServiceImpl(iVarianteRepository, iVarianteImagenRepository, iProductosRepository,
                iProductoImagenRepository, imageneClienteDisco, iImagenRepository, imagenPort,
                iPalabraClaveRepository, iCodigoBarrasRepository, errorGenerico);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        lenient().when(cacheManager.getCacheNames()).thenReturn(Collections.emptyList());
    }

    private Producto producto(int id, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Playera");
        p.setStock(stock);
        return p;
    }

    private Variantes variante(int id, Producto producto, int stock, char habilitado) {
        Variantes v = new Variantes();
        v.setId(id);
        v.setProducto(producto);
        v.setStock(stock);
        v.setHabilitado(habilitado);
        return v;
    }

    @Test
    void deleteByIdVariante_dejaLaVarianteEnCeroSinSubirElStockBase() {
        Producto producto = producto(100, 10);
        Variantes variante = variante(1, producto, 2, '1');

        when(iVarianteRepository.findById(1)).thenReturn(Optional.of(variante));
        when(iVarianteImagenRepository.findImagenIdsByVarianteIdIn(List.of(1))).thenReturn(List.of());
        when(iVarianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteByIdVariante(1);

        assertEquals(10, producto.getStock(), "el stock base es el total fisico: no sube por dar de baja una variante");
        assertEquals(0, variante.getStock(), "la variante dada de baja queda en 0, su stock vuelve al disponible");
        assertEquals('0', variante.getHabilitado());
        verify(iProductosRepository, never()).save(any());
    }

    @Test
    void habilitarDeshabilitarVariantesLote_alDeshabilitar_dejaLasVariantesEnCeroSinTocarElBase() {
        Producto producto = producto(100, 10);
        Variantes vA = variante(1, producto, 2, '1');
        Variantes vB = variante(2, producto, 3, '1');
        List<Integer> ids = List.of(1, 2);

        when(iVarianteRepository.findAllById(ids)).thenReturn(List.of(vA, vB));

        service.habilitarDeshabilitarVariantesLote(ids, false);

        assertEquals(10, producto.getStock(), "deshabilitar libera disponible, no crea stock nuevo en el producto");
        assertEquals(0, vA.getStock());
        assertEquals(0, vB.getStock());
        assertEquals('0', vA.getHabilitado());
        assertEquals('0', vB.getHabilitado());
        verify(iProductosRepository, never()).save(any());
    }

    @Test
    void habilitarDeshabilitarVariantesLote_alHabilitar_noTocaElStock() {
        Producto producto = producto(100, 10);
        // Ya estaba deshabilitada con stock 0 (el estado tras una baja) -- habilitar no le
        // regresa stock solo, eso requiere asignarselo de nuevo a mano.
        Variantes vA = variante(1, producto, 0, '0');
        List<Integer> ids = List.of(1);

        when(iVarianteRepository.findAllById(ids)).thenReturn(List.of(vA));

        service.habilitarDeshabilitarVariantesLote(ids, true);

        assertEquals(10, producto.getStock());
        assertEquals(0, vA.getStock());
        assertEquals('1', vA.getHabilitado());
        verify(iProductosRepository, never()).save(any());
    }

    /**
     * El caso del bug: editar el stock de una variante existente reparte del disponible, nunca
     * infla el stock base. Base 10 con la variante en 2, se edita a 5 -> base sigue 10.
     */
    @Test
    void guardarConImagenes_alEditarUnaVariante_noSubeElStockBaseDelProducto() throws Exception {
        Producto producto = producto(100, 10);
        Variantes existente = variante(1, producto, 2, '1');

        VarianteDetalle detalle = new VarianteDetalle();
        detalle.setId(1);
        detalle.setProductoId(100);
        detalle.setStock(5);

        when(iProductosRepository.findById(100)).thenReturn(Optional.of(producto));
        when(iVarianteRepository.findByProductoId(100)).thenReturn(List.of(existente));
        when(iVarianteRepository.findById(1)).thenReturn(Optional.of(existente));
        when(iProductosRepository.getReferenceById(100)).thenReturn(producto);
        when(iVarianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.guardarConImagenes(List.of(detalle));

        assertEquals(10, producto.getStock(), "editar el stock de una variante no crea stock nuevo en el producto");
        verify(iProductosRepository, never()).save(any());
    }
}
