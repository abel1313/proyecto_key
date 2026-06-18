package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ConfigurarRifaResumenDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConfiguracionRifaServiceImpl extends CrudAbstractServiceImpl<ConfigurarRifa, List<ConfigurarRifa>, Optional<ConfigurarRifa>, Integer, PginaDto<List<ConfigurarRifa>>> {

    private final IConfigurarRifaRepository iRifaRepository;
    private final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    private final IGanadorRifaRepository iGanadorRifaRepository;
    private final GanadorRifaServiceImpl ganadorRifaService;

    public ConfiguracionRifaServiceImpl(
            final IConfigurarRifaRepository iRifaRepository,
            final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository,
            final IGanadorRifaRepository iGanadorRifaRepository,
            final GanadorRifaServiceImpl ganadorRifaService,
            final ErrorGenerico eGenerico) {
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.iConfigurarRifaVarianteRepository = iConfigurarRifaVarianteRepository;
        this.iGanadorRifaRepository = iGanadorRifaRepository;
        this.ganadorRifaService = ganadorRifaService;
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

    public void desactivarVencidas() {
        // Desactiva rifas que llevan más de 1 día activas sin terminar
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        List<ConfigurarRifa> vencidas = iRifaRepository.findActivasVencidas(ayer);
        vencidas.forEach(r -> r.setActiva(false));
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