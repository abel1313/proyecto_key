package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.negocio.ContactosPublicosDto;
import com.ventas.key.mis.productos.dto.negocio.ContactosUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.HorarioUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioConfigDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioEstadoDto;
import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.repository.IConfiguracionNegocioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NegocioService {

    private static final int CONFIG_ID = 1;

    private final IConfiguracionNegocioRepository repo;

    public NegocioEstadoDto getEstado() {
        ConfiguracionNegocio config = obtenerConfig();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        // Los links de contacto solo se exponen al frontend cuando el negocio está cerrado.
        // El horario si se expone siempre (abierto o cerrado) -- el front lo necesita en los
        // dos casos para armar el texto de estado ("atendemos hasta las X" / "abrimos a las X").
        return NegocioEstadoDto.builder()
                .abierto(config.isAbierto())
                .whatsappUrl(config.isAbierto() ? null : config.getWhatsappUrl())
                .facebookUrl(config.isAbierto() ? null : config.getFacebookUrl())
                .instagramUrl(config.isAbierto() ? null : config.getInstagramUrl())
                .tiktokUrl(config.isAbierto() ? null : config.getTiktokUrl())
                .horaApertura(config.getHoraApertura() != null ? config.getHoraApertura().format(fmt) : null)
                .horaCierre(config.getHoraCierre() != null ? config.getHoraCierre().format(fmt) : null)
                .build();
    }

    /** Público — para el QR del ticket. A diferencia de getEstado(), siempre expone los links,
     *  sin importar si el negocio está abierto o cerrado. */
    public ContactosPublicosDto getContactosPublicos() {
        ConfiguracionNegocio config = obtenerConfig();
        return ContactosPublicosDto.builder()
                .whatsappUrl(config.getWhatsappUrl())
                .facebookUrl(config.getFacebookUrl())
                .instagramUrl(config.getInstagramUrl())
                .tiktokUrl(config.getTiktokUrl())
                .build();
    }

    public NegocioConfigDto getConfig() {
        ConfiguracionNegocio config = obtenerConfig();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return NegocioConfigDto.builder()
                .abierto(config.isAbierto())
                .whatsappUrl(config.getWhatsappUrl())
                .facebookUrl(config.getFacebookUrl())
                .instagramUrl(config.getInstagramUrl())
                .tiktokUrl(config.getTiktokUrl())
                .horaApertura(config.getHoraApertura() != null ? config.getHoraApertura().format(fmt) : null)
                .horaCierre(config.getHoraCierre() != null ? config.getHoraCierre().format(fmt) : null)
                .build();
    }

    @Transactional
    public NegocioConfigDto actualizarHorario(HorarioUpdateDto dto) {
        ConfiguracionNegocio config = obtenerConfig();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate hoy = LocalDate.now();
        if (dto.getHoraApertura() != null) {
            config.setHoraApertura(LocalDateTime.of(hoy, LocalTime.parse(dto.getHoraApertura(), fmt)));
        }
        if (dto.getHoraCierre() != null) {
            config.setHoraCierre(LocalDateTime.of(hoy, LocalTime.parse(dto.getHoraCierre(), fmt)));
        }
        config.setActualizadoEn(LocalDateTime.now());
        repo.save(config);
        return getConfig();
    }

    @Transactional
    public ConfiguracionNegocio abrir() {
        ConfiguracionNegocio config = obtenerConfig();
        config.setAbierto(true);
        config.setAbiertoDesde(LocalDateTime.now());
        config.setActualizadoEn(LocalDateTime.now());
        log.info("Negocio abierto a las {}", config.getAbiertoDesde());
        return repo.save(config);
    }

    @Transactional
    public ConfiguracionNegocio cerrar() {
        ConfiguracionNegocio config = obtenerConfig();
        config.setAbierto(false);
        config.setCerradoDesde(LocalDateTime.now());
        config.setActualizadoEn(LocalDateTime.now());
        log.info("Negocio cerrado a las {}", config.getCerradoDesde());
        return repo.save(config);
    }

    @Transactional
    public ConfiguracionNegocio actualizarContactos(ContactosUpdateDto dto) {
        ConfiguracionNegocio config = obtenerConfig();
        if (dto.getWhatsappUrl() != null) config.setWhatsappUrl(dto.getWhatsappUrl());
        if (dto.getFacebookUrl() != null) config.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null) config.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getTiktokUrl() != null) config.setTiktokUrl(dto.getTiktokUrl());
        config.setActualizadoEn(LocalDateTime.now());
        return repo.save(config);
    }

    @Transactional
    public void verificarAutoCierre() {
        ConfiguracionNegocio config = obtenerConfig();
        if (!config.isAbierto()) return;

        LocalTime horaLimite = config.getHoraCierre() != null
                ? config.getHoraCierre().toLocalTime()
                : LocalTime.of(21, 0);

        if (LocalTime.now().isAfter(horaLimite)) {
            config.setAbierto(false);
            config.setCerradoDesde(LocalDateTime.now());
            config.setActualizadoEn(LocalDateTime.now());
            repo.save(config);
            log.warn("Negocio cerrado automáticamente por hora límite {}", horaLimite);
        }
    }

    private ConfiguracionNegocio obtenerConfig() {
        // Ordenado por id ASC a proposito: sin esto, MySQL no garantiza el orden de un
        // findAll() sin ORDER BY, y "la primera fila" podia variar entre llamadas. Si alguna
        // vez llega a haber mas de una fila (ej. una carrera entre 2 requests casi simultaneos
        // la primera vez que se crea la config), eso hacia que se borrara la fila equivocada
        // segun cual "ganara" en ser la primera -- con riesgo real de perder datos ya guardados.
        // Con el orden fijo, siempre se conserva la fila mas antigua (menor id) de forma estable.
        List<ConfiguracionNegocio> configs = repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (configs.isEmpty()) {
            return repo.save(new ConfiguracionNegocio());
        }
        ConfiguracionNegocio horaNegocio =  configs.stream().findFirst().get();
        List<Integer> ids = configs.stream()
                .map(ConfiguracionNegocio::getId)
                .filter(id -> !id.equals(horaNegocio.getId()))
                .toList();
        if (!ids.isEmpty()) {
            repo.deleteAllById(ids);
        }
        return repo.findById(horaNegocio.getId())
                .orElseThrow(() -> new ExceptionDataNotFound("No se encontro el configuracio"));

    }
}