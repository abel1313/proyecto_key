package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.CompletarProductoDto;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Hotfix 2026-09-22: completar los datos de un borrador de Carga rapida escribia solo en Producto.
 *
 * La variante que crea crearBorrador() nace con unicamente producto+stock, asi que se quedaba sin
 * descripcion, color, marca ni contenido -- y la variante es lo que el cliente ve en la tienda y lo
 * que devuelven /v1/variantes/buscar y /v1/variantes/porProducto. El admin cargaba la foto, llenaba
 * la informacion, la veia bien en productos, y en la tienda el articulo salia vacio.
 */
@ExtendWith(MockitoExtension.class)
class CargaImagenesCompletarVarianteTest {

    @Mock private IProductosRepository iProductosRepository;
    @Mock private IVarianteRepository iVarianteRepository;
    @Mock private IProductoImagenRepository iProductoImagenRepository;
    @Mock private IVarianteImagenRepository iVarianteImagenRepository;
    @Mock private IImagenRepository iImagenRepository;
    @Mock private ICodigoBarrasRepository iCodigoBarrasRepository;
    @Mock private IPalabraClaveRepository iPalabraClaveRepository;
    @Mock private ImageneClienteDisco imageneClienteDisco;
    @Mock private CacheService cacheService;
    @Mock private RabbitTemplate rabbitTemplate;

    private CargaImagenesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CargaImagenesServiceImpl(iProductosRepository, iVarianteRepository,
                iProductoImagenRepository, iVarianteImagenRepository, iImagenRepository,
                iCodigoBarrasRepository, iPalabraClaveRepository, imageneClienteDisco,
                cacheService, rabbitTemplate);
        ReflectionTestUtils.setField(service, "endpointImagenes", "http://localhost/");
        lenient().when(iProductosRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Producto borrador() {
        CodigoBarra codigo = new CodigoBarra();
        codigo.setCodigoBarras("BRD-ABC123456789");
        Producto producto = new Producto();
        producto.setId(500);
        producto.setCodigoBarras(codigo);
        producto.setCodigoBarrasGenerado(true);
        producto.setStock(1);
        producto.setHabilitado('0');
        return producto;
    }

    /** La variante tal cual la deja crearBorrador(): solo producto y stock, todo lo demas null. */
    private Variantes varianteVacia(Producto producto) {
        Variantes variante = new Variantes();
        variante.setId(900);
        variante.setProducto(producto);
        variante.setStock(1);
        return variante;
    }

    @Test
    void completarProducto_bajaLosDatosDelFormularioALaVariante() {
        Producto producto = borrador();
        Variantes variante = varianteVacia(producto);

        CompletarProductoDto req = new CompletarProductoDto();
        req.setNombre("Jeans Short Brillo");
        req.setDescripcion("Bolsa");
        req.setColor("azul");
        req.setMarca("SIN MARCA");
        req.setContenido("500ml");

        when(iProductosRepository.findById(500)).thenReturn(Optional.of(producto));
        when(iVarianteRepository.findByProductoId(500)).thenReturn(List.of(variante));

        service.completarProducto(500, req);

        assertEquals("Bolsa", variante.getDescripcion(), "la descripcion del formulario tiene que bajar a la variante");
        assertEquals("azul", variante.getColor());
        assertEquals("SIN MARCA", variante.getMarca());
        assertEquals("500ml", variante.getContenidoNeto());
        // El nombre no se copia: la variante no tiene columna propia, lo hereda del producto.
        assertEquals("Jeans Short Brillo", producto.getNombre());
    }

    @Test
    void completarProducto_conPalabraClave_laAsignaTambienALaVariante() {
        Producto producto = borrador();
        Variantes variante = varianteVacia(producto);
        PalabraClave palabraClave = new PalabraClave();
        palabraClave.setId(7);

        CompletarProductoDto req = new CompletarProductoDto();
        req.setPalabraClaveId(7);

        when(iProductosRepository.findById(500)).thenReturn(Optional.of(producto));
        when(iVarianteRepository.findByProductoId(500)).thenReturn(List.of(variante));
        when(iPalabraClaveRepository.getReferenceById(7)).thenReturn(palabraClave);

        service.completarProducto(500, req);

        assertEquals(palabraClave, variante.getPalabraClave(),
                "sin la categoria en la variante el articulo no sale en las busquedas por palabra clave");
    }

    /**
     * Los campos que el admin no lleno llegan null y no tienen que pisar lo que la variante ya
     * tenga: /completar se puede llamar varias veces mientras se van cargando los datos.
     */
    @Test
    void completarProducto_camposNulos_noPisanLoQueLaVarianteYaTenia() {
        Producto producto = borrador();
        Variantes variante = varianteVacia(producto);
        variante.setDescripcion("descripcion previa");
        variante.setMarca("MARCA PREVIA");

        CompletarProductoDto req = new CompletarProductoDto();
        req.setColor("rojo");

        when(iProductosRepository.findById(500)).thenReturn(Optional.of(producto));
        when(iVarianteRepository.findByProductoId(500)).thenReturn(List.of(variante));

        service.completarProducto(500, req);

        assertEquals("rojo", variante.getColor());
        assertEquals("descripcion previa", variante.getDescripcion());
        assertEquals("MARCA PREVIA", variante.getMarca());
    }
}
