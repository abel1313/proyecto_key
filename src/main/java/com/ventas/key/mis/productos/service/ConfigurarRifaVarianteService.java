package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteRequest;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
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
    private final ImageneClienteDisco imageneClienteDisco;

    @Transactional
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

        // Imagen: tomar la primera imagen disponible
        List<VarianteImagen> imagenes = iVarianteImagenRepository.findByVarianteId(v.getId());
        if (!imagenes.isEmpty()) {
            try {
                Long imagenId = imagenes.get(0).getImagen().getId();
                ImagenDto img = imageneClienteDisco.getOne(imagenId);
                if (img != null && img.getImagen() != null) {
                    dto.setImagenBase64(Base64.getEncoder().encodeToString(img.getImagen()));
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener imagen para variante {}: {}", v.getId(), e.getMessage());
            }
        }

        return dto;
    }
}