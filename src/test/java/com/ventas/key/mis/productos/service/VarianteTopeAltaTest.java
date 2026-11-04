package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * QA 2026-11-04: llego un lote de miles de articulos identicos (productoId 265, stock 5 cada uno)
 * a guardarConImagenes y se guardaron 2226, comiendose el stock libre del producto. Ahora una
 * peticion con mas de 200 articulos se rechaza antes de tocar la base.
 */
class VarianteTopeAltaTest {

    private IProductosRepository productoRepo;
    private IVarianteRepository varianteRepo;
    private VarianteServiceImpl service;

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

    private static List<VarianteDetalle> lote(int n) {
        List<VarianteDetalle> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            VarianteDetalle d = new VarianteDetalle();
            d.setProductoId(265);
            d.setStock(5);
            l.add(d);
        }
        return l;
    }

    @Test
    @DisplayName("201 articulos en una peticion se rechazan con mensaje claro y sin tocar la base")
    void masDeDoscientosSeRechaza() {
        assertThatThrownBy(() -> service.guardarConImagenes(lote(201)))
                .isInstanceOf(ExceptionErrorInesperado.class)
                .hasMessageContaining("201")
                .hasMessageContaining("200");
        verifyNoInteractions(productoRepo, varianteRepo);
    }

    @Test
    @DisplayName("Un lote de miles (el de QA) tambien se rechaza")
    void loteDeMilesSeRechaza() {
        assertThatThrownBy(() -> service.guardarConImagenes(lote(5000)))
                .isInstanceOf(ExceptionErrorInesperado.class)
                .hasMessageContaining("maximo");
        verifyNoInteractions(productoRepo, varianteRepo);
    }
}
