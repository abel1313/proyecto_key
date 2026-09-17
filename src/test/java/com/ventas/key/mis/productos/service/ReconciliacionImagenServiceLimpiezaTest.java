package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.hexagonal.infraestructura.ImagenProductoClienteVPS;
import com.ventas.key.mis.productos.repository.IImagenPresentacionRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.ILogoRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// imagen_presentacion (login/registro) y logo escriben sus archivos en el MISMO directorio que las
// imagenes de producto. La limpieza de huerfanos de las 4 AM arma su lista de nombres validos desde
// los repositorios: cuando solo consultaba `imagen`, borraba los archivos de las otras dos tablas
// todas las noches y habia que volver a subir las imagenes de presentacion a diario. Este test fija
// ese contrato: si alguien agrega otra tabla que escriba en la misma ruta y no la suma aqui, el bug
// vuelve -- y con archivos de usuarios, no de prueba.
@ExtendWith(MockitoExtension.class)
class ReconciliacionImagenServiceLimpiezaTest {

    @Mock private IProductosRepository iProductosRepository;
    @Mock private IVarianteRepository iVarianteRepository;
    @Mock private IProductoImagenRepository iProductoImagenRepository;
    @Mock private IVarianteImagenRepository iVarianteImagenRepository;
    @Mock private IImagenRepository iImagenRepository;
    @Mock private IImagenPresentacionRepository iImagenPresentacionRepository;
    @Mock private ILogoRepository iLogoRepository;
    @Mock private ImagenProductoClienteVPS imagenProductoClienteVPS;

    @TempDir Path directorioImagenes;

    private ReconciliacionImagenService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliacionImagenService(
                iProductosRepository, iVarianteRepository, iProductoImagenRepository,
                iVarianteImagenRepository, iImagenRepository, iImagenPresentacionRepository,
                iLogoRepository, imagenProductoClienteVPS);
        ReflectionTestUtils.setField(service, "rutaImagenes", directorioImagenes.toString());
    }

    // La limpieza perdona los archivos creados dentro de su ventana de gracia. No se puede
    // envejecer un archivo en el test (setAttribute("creationTime") no falla pero tampoco cambia
    // nada en varios filesystems de Linux), asi que en su lugar se cierra la ventana a 0: sin esto
    // todos los archivos sobreviven y el test pasaria incluso con el bug presente.
    private void cerrarVentanaDeGracia() {
        ReflectionTestUtils.setField(service, "ventanaGraciaSegundos", 0L);
    }

    private Path archivo(String nombre) throws IOException {
        Path archivo = directorioImagenes.resolve(nombre);
        Files.writeString(archivo, "contenido");
        return archivo;
    }

    @Test
    void conservaLosArchivosDePresentacionYDeLogo_yBorraSoloLosHuerfanos() throws IOException {
        cerrarVentanaDeGracia();
        Path producto      = archivo("uuid-producto.jpg");
        Path presentacion  = archivo("uuid-login.jpg");
        Path logo          = archivo("uuid-logo.png");
        Path huerfano      = archivo("uuid-nadie-lo-referencia.jpg");

        when(iImagenRepository.findAllBase64()).thenReturn(List.of("uuid-producto.jpg"));
        when(iImagenPresentacionRepository.findAllNombresArchivo()).thenReturn(List.of("uuid-login.jpg"));
        when(iLogoRepository.findAllNombresArchivo()).thenReturn(List.of("uuid-logo.png"));

        service.limpiarDiscoDia();

        assertThat(presentacion).as("la imagen del login no debe borrarse").exists();
        assertThat(logo).as("el logo no debe borrarse").exists();
        assertThat(producto).as("la imagen de producto no debe borrarse").exists();
        assertThat(huerfano).as("un archivo que nadie referencia si se borra").doesNotExist();

        assertThat(service.getUltimoResultado().getArchivosEliminadosDisco()).isEqualTo(1);
    }

    @Test
    void noBorraNadaRecienSubido() throws IOException {
        Path recien = archivo("uuid-subida-hace-un-minuto.jpg");

        when(iImagenRepository.findAllBase64()).thenReturn(List.of());
        when(iImagenPresentacionRepository.findAllNombresArchivo()).thenReturn(List.of());
        when(iLogoRepository.findAllNombresArchivo()).thenReturn(List.of());

        service.limpiarDiscoDia();

        assertThat(recien).exists();
    }
}
