package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConfiguracionRifaServiceImpl extends CrudAbstractServiceImpl<ConfigurarRifa, List<ConfigurarRifa>, Optional<ConfigurarRifa>, Integer, PginaDto<List<ConfigurarRifa>>> {

    private final IConfigurarRifaRepository iRifaRepository;

    public ConfiguracionRifaServiceImpl(
            final IConfigurarRifaRepository iRifaRepository,
            final ErrorGenerico eGenerico) {
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
    }

    public List<ConfigurarRifa> buscarActivas() {
        return iRifaRepository.findByActivaTrue();
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