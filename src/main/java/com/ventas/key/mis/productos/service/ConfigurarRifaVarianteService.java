package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.CacheNames;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteEditarRequest;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteRequest;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurarRifaVarianteService {

    private final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;

    @Value("${api.imagenes}")
    private String endpointImagenes;

    @PostConstruct
    public void normalizarEndpoints() {
        if (!endpointImagenes.endsWith("/")) endpointImagenes = endpointImagenes + "/";
    }

    /**
     * Reservar un premio mueve stock (lo descuenta al agregar, lo devuelve al eliminar/editar), asi
     * que invalida los mismos caches que una venta. Sin esto la busqueda de premios
     * (/tienda/v1/buscar-filtrado, que exige stock > 0) se quedaba con el resultado viejo hasta 1h:
     * al quitar un premio la variante no reaparecia, y el unico flujo que refrescaba el cache era
     * volver a subir la imagen (ImagenServiceImpl es el otro que lo desaloja).
     */
    @Transactional
    @CacheEvict(value = {CacheNames.PRODUCTOS, CacheNames.PRODUCTOS_BUSQUEDA, CacheNames.PRODUCTO_DETALLE,
            CacheNames.VARIANTES, CacheNames.VARIANTES_NOMBRE, CacheNames.VARIANTES_CODIGO_BARRAS},
            allEntries = true)
    public ConfigurarRifaVarianteDto agregar(ConfigurarRifaVarianteRequest req) {
        ConfigurarRifa rifa = iConfigurarRifaRepository.findById(req.getConfigurarRifaId())
                .orElseThrow(() -> new ExceptionDataNotFound("Rifa no encontrada"));

        if (!Boolean.TRUE.equals(rifa.getActiva())) {
            throw new ExceptionErrorInesperado("La rifa no está activa");
        }

        String palabraClave = req.getPalabraClave().toUpperCase().trim();
        Optional<ConfigurarRifaVariante> existente = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdAndPalabraClave(req.getConfigurarRifaId(), palabraClave);

        if (existente.isPresent()) {
            if (!Boolean.TRUE.equals(rifa.getEsPrueba())) {
                throw new ExceptionErrorInesperado("La palabraClave '" + req.getPalabraClave() + "' ya existe en esta rifa");
            }
            // Rifa de prueba: re-test con la misma palabraClave, se actualiza la configuración existente
            return actualizarExistente(existente.get(), req);
        }

        Variantes variante = iVarianteRepository.findById(req.getVarianteId())
                .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada"));

        if (variante.getStock() < 1) {
            throw new ExceptionErrorInesperado("La variante no tiene stock disponible");
        }

        variante.setStock(variante.getStock() - 1);
        iVarianteRepository.save(variante);

        ConfigurarRifaVariante crv = new ConfigurarRifaVariante();
        crv.setConfigurarRifa(rifa);
        crv.setVariante(variante);
        crv.setPalabraClave(palabraClave);
        crv.setGiroGanador(req.getGiroGanador());
        crv.setOrden(req.getOrden());
        crv.setPermitirNuevos(req.isPermitirNuevos());
        crv.setStockReservado(1);

        ConfigurarRifaVariante guardada = iConfigurarRifaVarianteRepository.save(crv);
        log.info("Variante {} agregada a rifa {} con palabraClave={}", variante.getId(), rifa.getId(), crv.getPalabraClave());
        return toDto(guardada);
    }

    private ConfigurarRifaVarianteDto actualizarExistente(ConfigurarRifaVariante crv, ConfigurarRifaVarianteRequest req) {
        if (!crv.getVariante().getId().equals(req.getVarianteId())) {
            Variantes anterior = crv.getVariante();
            anterior.setStock(anterior.getStock() + crv.getStockReservado());
            iVarianteRepository.save(anterior);

            Variantes nueva = iVarianteRepository.findById(req.getVarianteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada"));
            if (nueva.getStock() < 1) {
                throw new ExceptionErrorInesperado("La variante no tiene stock disponible");
            }
            nueva.setStock(nueva.getStock() - 1);
            iVarianteRepository.save(nueva);

            crv.setVariante(nueva);
            crv.setStockReservado(1);
        }

        crv.setGiroGanador(req.getGiroGanador());
        crv.setOrden(req.getOrden());
        crv.setPermitirNuevos(req.isPermitirNuevos());

        ConfigurarRifaVariante guardada = iConfigurarRifaVarianteRepository.save(crv);
        log.info("Variante {} actualizada (re-test, rifa de prueba {}) con palabraClave={}",
                guardada.getVariante().getId(), guardada.getConfigurarRifa().getId(), guardada.getPalabraClave());
        return toDto(guardada);
    }

    @Transactional
    @CacheEvict(value = {CacheNames.PRODUCTOS, CacheNames.PRODUCTOS_BUSQUEDA, CacheNames.PRODUCTO_DETALLE,
            CacheNames.VARIANTES, CacheNames.VARIANTES_NOMBRE, CacheNames.VARIANTES_CODIGO_BARRAS},
            allEntries = true)
    public void eliminar(Integer id) {
        ConfigurarRifaVariante crv = iConfigurarRifaVarianteRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Configuración de variante no encontrada"));

        Variantes variante = crv.getVariante();
        variante.setStock(variante.getStock() + crv.getStockReservado());
        iVarianteRepository.save(variante);

        iConfigurarRifaVarianteRepository.delete(crv);
        log.info("Variante {} eliminada de rifa {}, stock restaurado", variante.getId(), crv.getConfigurarRifa().getId());
    }

    public List<ConfigurarRifaVarianteDto> listarPorRifa(Integer rifaId) {
        return iConfigurarRifaVarianteRepository.findByConfigurarRifaIdOrderByOrdenAsc(rifaId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<String> obtenerPalabrasClave(Integer rifaId) {
        return iConfigurarRifaVarianteRepository.findPalabrasClave(rifaId);
    }

    /**
     * Edita un premio ya guardado sin tener que borrarlo y recrearlo. Se aplica solo lo que
     * venga distinto de null. Cambiar el producto mueve la reserva de stock: devuelve la del
     * anterior y descuenta una del nuevo, igual que hace {@link #actualizarExistente}.
     */
    @Transactional
    @CacheEvict(value = {CacheNames.PRODUCTOS, CacheNames.PRODUCTOS_BUSQUEDA, CacheNames.PRODUCTO_DETALLE,
            CacheNames.VARIANTES, CacheNames.VARIANTES_NOMBRE, CacheNames.VARIANTES_CODIGO_BARRAS},
            allEntries = true)
    public ConfigurarRifaVarianteDto editar(Integer id, ConfigurarRifaVarianteEditarRequest req) {
        ConfigurarRifaVariante crv = iConfigurarRifaVarianteRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Configuración de variante no encontrada"));

        if (req.getGiroGanador() != null) {
            if (req.getGiroGanador() < 1) {
                throw new ExceptionErrorInesperado("El giro ganador debe ser 1 o más");
            }
            crv.setGiroGanador(req.getGiroGanador());
        }
        if (req.getOrden() != null) {
            crv.setOrden(req.getOrden());
        }
        if (req.getPermitirNuevos() != null) {
            crv.setPermitirNuevos(req.getPermitirNuevos());
        }
        if (req.getPalabraClave() != null && !req.getPalabraClave().isBlank()) {
            String nueva = req.getPalabraClave().toUpperCase().trim();
            if (!nueva.equals(crv.getPalabraClave())
                    && iConfigurarRifaVarianteRepository.existsByConfigurarRifaIdAndPalabraClave(
                            crv.getConfigurarRifa().getId(), nueva)) {
                throw new ExceptionErrorInesperado("La palabraClave ya existe en esta rifa");
            }
            crv.setPalabraClave(nueva);
        }
        if (req.getVarianteId() != null && !req.getVarianteId().equals(crv.getVariante().getId())) {
            Variantes anterior = crv.getVariante();
            anterior.setStock(anterior.getStock() + crv.getStockReservado());
            iVarianteRepository.save(anterior);

            Variantes nueva = iVarianteRepository.findById(req.getVarianteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada"));
            if (nueva.getStock() < 1) {
                throw new ExceptionErrorInesperado("La variante no tiene stock disponible");
            }
            nueva.setStock(nueva.getStock() - 1);
            iVarianteRepository.save(nueva);

            crv.setVariante(nueva);
            crv.setStockReservado(1);
        }

        ConfigurarRifaVarianteDto dto = toDto(iConfigurarRifaVarianteRepository.save(crv));
        log.info("Premio {} de la rifa {} editado (giroGanador={}, orden={})",
                id, crv.getConfigurarRifa().getId(), crv.getGiroGanador(), crv.getOrden());
        return dto;
    }

    @Transactional
    public ConfigurarRifaVarianteDto actualizarPalabraClave(Integer id, String nuevaPalabraClave) {
        ConfigurarRifaVariante crv = iConfigurarRifaVarianteRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Configuración de variante no encontrada"));

        if (iConfigurarRifaVarianteRepository.existsByConfigurarRifaIdAndPalabraClave(
                crv.getConfigurarRifa().getId(), nuevaPalabraClave.toUpperCase().trim())) {
            throw new ExceptionErrorInesperado("La palabraClave ya existe en esta rifa");
        }

        crv.setPalabraClave(nuevaPalabraClave.toUpperCase().trim());
        return toDto(iConfigurarRifaVarianteRepository.save(crv));
    }

    public ConfigurarRifaVarianteDto toDto(ConfigurarRifaVariante crv) {
        ConfigurarRifaVarianteDto dto = new ConfigurarRifaVarianteDto();
        dto.setId(crv.getId());
        dto.setPalabraClave(crv.getPalabraClave());
        dto.setGiroGanador(crv.getGiroGanador());
        dto.setOrden(crv.getOrden());
        dto.setPermitirNuevos(crv.isPermitirNuevos());
        dto.setStockReservado(crv.getStockReservado());
        dto.setVariante(toVarianteResumen(crv.getVariante()));
        return dto;
    }

    private VarianteResumenDto toVarianteResumen(Variantes v) {
        VarianteResumenDto dto = new VarianteResumenDto();
        dto.setId(v.getId());
        dto.setTalla(v.getTalla());
        dto.setColor(v.getColor());
        dto.setDescripcion(v.getDescripcion());
        dto.setPresentacion(v.getPresentacion());
        dto.setStock(v.getStock());
        dto.setMarca(v.getMarca());
        dto.setContenidoNeto(v.getContenidoNeto());

        Producto producto = v.getProducto();
        if (producto != null) {
            dto.setNombreProducto(producto.getNombre());
            dto.setPrecio(producto.getPrecioVenta() != null ? producto.getPrecioVenta() : 0.0);
            dto.setCodigoBarras(Optional.ofNullable(producto.getCodigoBarras())
                    .map(CodigoBarra::getCodigoBarras).orElse(""));
        }

        // Solo la URL del micro, nunca el binario: antes esto bajaba la imagen server-to-server y la
        // mandaba en base64 dentro del JSON, que pesa ~33% mas que el binario y ademas el navegador
        // no lo puede cachear -- una lista de premios se comia el plan de datos del celular. De pilon
        // esa llamada fallaba de vez en cuando (timeout, micro caido) y el premio salia sin foto
        // aunque en modelos se viera bien. Mismo orden (principal primero) que el listado de busqueda.
        List<Object[]> filas = iVarianteImagenRepository.findIdsPrimeraImagenByVarianteIdIn(List.of(v.getId()));
        if (!filas.isEmpty()) {
            dto.setImagenUrl(endpointImagenes + "v1/imagenes/thumbnail/" + (Long) filas.get(0)[1]);
        }

        return dto;
    }
}