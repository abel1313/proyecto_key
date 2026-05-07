package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ConfigurarRifaResumenDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import org.springframework.stereotype.Service;

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

    public ConfiguracionRifaServiceImpl(
            final IConfigurarRifaRepository iRifaRepository,
            final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository,
            final IGanadorRifaRepository iGanadorRifaRepository,
            final ErrorGenerico eGenerico) {
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.iConfigurarRifaVarianteRepository = iConfigurarRifaVarianteRepository;
        this.iGanadorRifaRepository = iGanadorRifaRepository;
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
                totalVariantes, variantesSorteadas);
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
}