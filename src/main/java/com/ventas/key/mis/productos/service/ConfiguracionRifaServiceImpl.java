package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ConfigurarRifaPatchDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaResumenDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConfiguracionRifaServiceImpl extends CrudAbstractServiceImpl<ConfigurarRifa, List<ConfigurarRifa>, Optional<ConfigurarRifa>, Integer, PginaDto<List<ConfigurarRifa>>> {

    private final IConfigurarRifaRepository iRifaRepository;
    private final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    private final IGanadorRifaRepository iGanadorRifaRepository;
    private final GanadorRifaServiceImpl ganadorRifaService;
    private final IVarianteRepository iVarianteRepository;

    public ConfiguracionRifaServiceImpl(
            final IConfigurarRifaRepository iRifaRepository,
            final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository,
            final IGanadorRifaRepository iGanadorRifaRepository,
            final GanadorRifaServiceImpl ganadorRifaService,
            final ErrorGenerico eGenerico,
            final IVarianteRepository iVarianteRepository) {
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.iConfigurarRifaVarianteRepository = iConfigurarRifaVarianteRepository;
        this.iGanadorRifaRepository = iGanadorRifaRepository;
        this.ganadorRifaService = ganadorRifaService;
        this.iVarianteRepository = iVarianteRepository;
    }

    public List<ConfigurarRifa> buscarActivas() {
        return iRifaRepository.findByActivaTrue();
    }

    public List<ConfigurarRifaResumenDto> buscarActivasResumen() {
        return iRifaRepository.findByActivaTrue().stream().map(this::toResumen).toList();
    }

    public List<ConfigurarRifaResumenDto> buscarActivasHoyResumen() {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);
        return iRifaRepository.findActivasDelDia(inicioDia, finDia).stream().map(this::toResumen).toList();
    }

    private ConfigurarRifaResumenDto toResumen(ConfigurarRifa rifa) {
        int totalVariantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(rifa.getId()).size();
        long variantesSorteadas = iGanadorRifaRepository.countGanadoresByRifaId(rifa.getId());
        return new ConfigurarRifaResumenDto(
                rifa.getId(), rifa.getFechaHoraLimite(), rifa.getActiva(),
                totalVariantes, variantesSorteadas,
                rifa.getTipo(), rifa.getMesReferencia(), rifa.getEsPrueba());
    }

    public List<ConfigurarRifa> buscarActivasHoy() {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);
        return iRifaRepository.findActivasDelDia(inicioDia, finDia);
    }

    @Transactional
    public void desactivarVencidas() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        List<ConfigurarRifa> vencidas = iRifaRepository.findActivasVencidas(ayer);
        for (ConfigurarRifa r : vencidas) {
            r.setActiva(false);
            boolean devuelveStock = Boolean.TRUE.equals(r.getEsPrueba())
                    || ConfigurarRifa.TipoRifa.DIARIA.equals(r.getTipo());
            if (devuelveStock) {
                List<ConfigurarRifaVariante> variantesRifa = iConfigurarRifaVarianteRepository
                        .findByConfigurarRifaIdOrderByOrdenAsc(r.getId());
                for (ConfigurarRifaVariante crv : variantesRifa) {
                    Variantes variante = crv.getVariante();
                    variante.setStock(variante.getStock() + crv.getStockReservado());
                    iVarianteRepository.save(variante);
                }
                log.info("Rifa {} ({}) vencida: stock devuelto a {} variante(s)",
                        r.getId(), r.getTipo(), variantesRifa.size());
            }
        }
        iRifaRepository.saveAll(vencidas);
    }

    @Transactional
    public ConfigurarRifa toggleEsPrueba(int id, boolean esPrueba) {
        ConfigurarRifa config = iRifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        boolean eraPrueba = Boolean.TRUE.equals(config.getEsPrueba());
        if (eraPrueba && !esPrueba) {
            // Pasar de prueba -> real: limpiar giros de la demo y reactivar la rifa
            ganadorRifaService.reiniciar(id, false);
            config = iRifaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));
        }

        config.setEsPrueba(esPrueba);
        return iRifaRepository.save(config);
    }

    @Transactional
    public ConfigurarRifa actualizarConfiguracion(int id, ConfigurarRifaPatchDto patch) {
        ConfigurarRifa config = iRifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        if (patch.getTipo() != null && patch.getTipo() != config.getTipo()) {
            boolean tieneVariantes = !iConfigurarRifaVarianteRepository
                    .findByConfigurarRifaIdOrderByOrdenAsc(id).isEmpty();
            if (tieneVariantes) {
                throw new RuntimeException(
                        "No se puede cambiar el tipo de rifa porque ya tiene variantes configuradas. Elimina las variantes primero.");
            }
        }

        if (patch.getFechaHoraLimite() != null) {
            config.setFechaHoraLimite(patch.getFechaHoraLimite());
        }
        if (patch.getTipo() != null) {
            config.setTipo(patch.getTipo());
            if (patch.getTipo() != ConfigurarRifa.TipoRifa.MENSUAL) {
                config.setMesReferencia(null);
            }
        }
        if (patch.getMesReferencia() != null) {
            config.setMesReferencia(patch.getMesReferencia().isBlank() ? null : patch.getMesReferencia());
        }

        return iRifaRepository.save(config);
    }

    public List<ConfigurarRifaResumenDto> buscar(LocalDate desde, LocalDate hasta,
                                                   ConfigurarRifa.TipoRifa tipo, String mesReferencia) {
        if (desde == null && hasta == null && tipo == null && mesReferencia == null) {
            return buscarActivasHoyResumen();
        }

        List<ConfigurarRifa> resultado;
        if (desde != null || hasta != null) {
            LocalDate desdeFecha = desde != null ? desde : hasta;
            LocalDate hastaFecha = hasta != null ? hasta : desde;
            LocalDateTime inicio = desdeFecha.atStartOfDay();
            LocalDateTime fin = hastaFecha.atTime(LocalTime.MAX);
            resultado = iRifaRepository.findByFechaHoraLimiteBetween(inicio, fin);
        } else {
            resultado = iRifaRepository.findAll();
        }

        if (tipo != null) {
            resultado = resultado.stream().filter(r -> r.getTipo() == tipo).toList();
        }
        if (mesReferencia != null && !mesReferencia.isBlank()) {
            resultado = resultado.stream().filter(r -> mesReferencia.equals(r.getMesReferencia())).toList();
        }

        return resultado.stream().map(this::toResumen).toList();
    }
}