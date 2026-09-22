package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
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
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La carga rapida sube la foto tal cual llega del celular. El micro rechaza con 400 un archivo
 * cuyo nombre no coincide con sus bytes, asi que el nombre tiene que salir corregido.
 */
class CargaRapidaNombreArchivoTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0};

    private ImageneClienteDisco micro;
    private CargaImagenesServiceImpl service;

    @BeforeEach
    void setUp() {
        micro = mock(ImageneClienteDisco.class);
        ImagenDto subida = new ImagenDto();
        subida.setId(1L);
        when(micro.save(any())).thenReturn(List.of(subida));
        service = new CargaImagenesServiceImpl(
                mock(IProductosRepository.class), mock(IVarianteRepository.class),
                mock(IProductoImagenRepository.class), mock(IVarianteImagenRepository.class),
                mock(IImagenRepository.class), mock(ICodigoBarrasRepository.class),
                mock(IPalabraClaveRepository.class), micro, mock(CacheService.class),
                mock(RabbitTemplate.class));
    }

    @SuppressWarnings("unchecked")
    private String nombreEnviado(byte[] bytes, String nombre) {
        ReflectionTestUtils.invokeMethod(service, "subirImagenAMicro", bytes, nombre);
        ArgumentCaptor<MultiValueMap<String, ?>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(micro).save(captor.capture());
        HttpEntity<?> parte = (HttpEntity<?>) captor.getValue().getFirst("files");
        HttpHeaders headers = parte.getHeaders();
        assertThat(headers.getContentDisposition().getFilename())
                .as("no debe quedar un Content-Disposition manual que pise el nombre corregido")
                .isNull();
        return ((Resource) parte.getBody()).getFilename();
    }

    @Test
    @DisplayName("un .png que por dentro es JPEG sale como .jpg")
    void pngQueEsJpegSeRenombra() {
        assertThat(nombreEnviado(JPEG, "1000122954.png")).isEqualTo("1000122954.jpg");
    }

    @Test
    @DisplayName("un .jpg que por dentro es PNG sale como .png (error reportado en produccion 2026-09-22)")
    void jpgQueEsPngSeRenombra() {
        assertThat(nombreEnviado(PNG, "nombre.jpg")).isEqualTo("nombre.png");
    }

    @Test
    @DisplayName("un PNG de verdad conserva su nombre")
    void pngRealConservaNombre() {
        assertThat(nombreEnviado(PNG, "captura.png")).isEqualTo("captura.png");
    }

    @Test
    @DisplayName("un JPEG con nombre correcto no se toca")
    void jpegCorrectoNoSeToca() {
        assertThat(nombreEnviado(JPEG, "1000284016.jpg")).isEqualTo("1000284016.jpg");
    }
}
