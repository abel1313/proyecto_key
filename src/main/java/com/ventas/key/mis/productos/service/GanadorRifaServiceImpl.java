package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.SorteoEstadoDto;
import com.ventas.key.mis.productos.models.SorteoResultadoDto;
import com.ventas.key.mis.productos.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GanadorRifaServiceImpl extends CrudAbstractServiceImpl<GanadorRifa, List<GanadorRifa>, Optional<GanadorRifa>, Integer, PginaDto<List<GanadorRifa>>> {

    private final IGanadorRifaRepository iGanadorRifaRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;
    private final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    private final IHistorialRifaVarianteRepository iHistorialRifaVarianteRepository;
    private final IConcursanteRepository iConcursanteRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConfigurarRifaVarianteService configurarRifaVarianteService;

    public GanadorRifaServiceImpl(
            final IGanadorRifaRepository iGanadorRifaRepository,
            final ErrorGenerico eGenerico,
            final IConfigurarRifaRepository iConfigurarRifaRepository,
            final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository,
            final IHistorialRifaVarianteRepository iHistorialRifaVarianteRepository,
            final IConcursanteRepository iConcursanteRepository,
            final SimpMessagingTemplate messagingTemplate,
            final ConfigurarRifaVarianteService configurarRifaVarianteService) {
        super(iGanadorRifaRepository, eGenerico);
        this.iGanadorRifaRepository = iGanadorRifaRepository;
        this.iConfigurarRifaRepository = iConfigurarRifaRepository;
        this.iConfigurarRifaVarianteRepository = iConfigurarRifaVarianteRepository;
        this.iHistorialRifaVarianteRepository = iHistorialRifaVarianteRepository;
        this.iConcursanteRepository = iConcursanteRepository;
        this.messagingTemplate = messagingTemplate;
        this.configurarRifaVarianteService = configurarRifaVarianteService;
    }

    @Transactional
    public SorteoResultadoDto sortear(int configurarRifaId) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new RuntimeException("Esta rifa ya fue completada o está inactiva");
        }

        List<ConfigurarRifaVariante> variantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        if (variantes.isEmpty()) {
            throw new RuntimeException("La rifa no tiene variantes configuradas");
        }

        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(configurarRifaId);
        if (ganadoresDeclarados >= variantes.size()) {
            throw new RuntimeException("Todas las variantes ya fueron sorteadas");
        }

        ConfigurarRifaVariante varianteActual = variantes.get((int) ganadoresDeclarados);
        long girosHechos = iGanadorRifaRepository.countDescartadosByVarianteRifaId(varianteActual.getId());
        long giroActual = girosHechos + 1;

        // Elegibles: no descartados y con palabraClave igual a la variante actual
        List<Concursante> elegibles = iConcursanteRepository
                .findByConfigurarRifaIdAndDescartadoFalseAndPalabraClave(
                        configurarRifaId, varianteActual.getPalabraClave());

        if (elegibles.isEmpty()) {
            throw new RuntimeException("No hay concursantes elegibles para la variante con palabraClave='"
                    + varianteActual.getPalabraClave() + "'");
        }

        Concursante seleccionado = elegibles.get(new Random().nextInt(elegibles.size()));
        boolean esGanador = giroActual >= varianteActual.getGiroGanador();

        seleccionado.setDescartado(true);
        iConcursanteRepository.save(seleccionado);

        GanadorRifa gr = new GanadorRifa();
        gr.setConcursante(seleccionado);
        gr.setConfigurarRifaVariante(varianteActual);
        gr.setDescartado(!esGanador);

        if (esGanador) {
            // Si era la última variante, cerrar la rifa
            if (ganadoresDeclarados + 1 >= variantes.size()) {
                config.setActiva(false);
                iConfigurarRifaRepository.save(config);
                log.info("Rifa {} completada", configurarRifaId);
            }
        }

        GanadorRifa guardado = iGanadorRifaRepository.save(gr);

        boolean rifaTerminada = !config.getActiva();
        ConfigurarRifaVarianteDto varianteDto = configurarRifaVarianteService.toDto(varianteActual);

        SorteoResultadoDto resultado = new SorteoResultadoDto();
        resultado.setDescartado(!esGanador);
        resultado.setConcursante(seleccionado);
        resultado.setVarianteActual(varianteDto);
        resultado.setRifaTerminada(rifaTerminada);

        messagingTemplate.convertAndSend("/topic/ruleta", resultado);
        log.info("Sorteo rifa={} variante={} palabraClave={} giro={}/{} esGanador={}",
                configurarRifaId, varianteActual.getOrden(), varianteActual.getPalabraClave(),
                giroActual, varianteActual.getGiroGanador(), esGanador);
        return resultado;
    }

    @Transactional
    public SorteoEstadoDto continuarVariante(int configurarRifaId, String modo) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        List<ConfigurarRifaVariante> variantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(configurarRifaId);

        if (ganadoresDeclarados >= variantes.size()) {
            throw new RuntimeException("No hay siguiente variante");
        }

        ConfigurarRifaVariante varianteAnterior = variantes.get((int) ganadoresDeclarados - 1);
        ConfigurarRifaVariante varianteSiguiente = variantes.get((int) ganadoresDeclarados);

        HistorialRifaVariante historial = new HistorialRifaVariante();
        historial.setConfigurarRifa(config);
        historial.setConfigurarRifaVariante(varianteAnterior);
        historial.setOrden(varianteAnterior.getOrden());
        historial.setModoContinuacion(HistorialRifaVariante.ModoContinuacion.valueOf(modo.toUpperCase()));

        // Ganador de la variante anterior
        List<GanadorRifa> ganadoresAnteriores = iGanadorRifaRepository.findGanadoresByRifaId(configurarRifaId);
        ganadoresAnteriores.stream()
                .filter(g -> g.getConfigurarRifaVariante().getId().equals(varianteAnterior.getId()))
                .findFirst()
                .ifPresent(g -> historial.setConcursanteGanador(g.getConcursante()));

        iHistorialRifaVarianteRepository.save(historial);

        // Aplicar modo para la siguiente variante
        switch (modo.toUpperCase()) {
            case "RESTANTES" -> {
                // Los no descartados de la variante anterior pasan con la nueva palabraClave
                List<Concursante> restantes = iConcursanteRepository
                        .findByConfigurarRifaIdAndDescartadoFalseAndPalabraClave(
                                configurarRifaId, varianteAnterior.getPalabraClave());
                restantes.forEach(c -> c.setPalabraClave(varianteSiguiente.getPalabraClave()));
                iConcursanteRepository.saveAll(restantes);
            }
            case "CERO" -> {
                // Todos los de la variante anterior resetean y pasan con la nueva palabraClave
                List<Concursante> todos = iConcursanteRepository
                        .findByConfigurarRifaIdAndPalabraClave(
                                configurarRifaId, varianteAnterior.getPalabraClave());
                todos.forEach(c -> {
                    c.setDescartado(false);
                    c.setPalabraClave(varianteSiguiente.getPalabraClave());
                });
                iConcursanteRepository.saveAll(todos);
            }
            case "NUEVOS" -> {
                // Solo los nuevos participarán. Los anteriores quedan como están.
                log.info("Modo NUEVOS: solo participantes nuevos con palabraClave={} irán al sorteo",
                        varianteSiguiente.getPalabraClave());
            }
            default -> throw new RuntimeException("Modo inválido: " + modo + ". Usar RESTANTES, CERO o NUEVOS");
        }

        return obtenerEstado(configurarRifaId);
    }

    public SorteoEstadoDto obtenerEstado(int configurarRifaId) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        List<ConfigurarRifaVariante> variantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(configurarRifaId);
        boolean terminada = !variantes.isEmpty() && ganadoresDeclarados >= variantes.size();

        ConfigurarRifaVariante varianteActual = null;
        int giroActual = 0;
        int giroGanador = 0;
        List<Concursante> elegibles = List.of();
        List<Concursante> descartados = List.of();

        if (!terminada && !variantes.isEmpty()) {
            varianteActual = variantes.get((int) ganadoresDeclarados);
            giroActual = (int) iGanadorRifaRepository
                    .countDescartadosByVarianteRifaId(varianteActual.getId()) + 1;
            giroGanador = varianteActual.getGiroGanador();

            String palabraActual = varianteActual.getPalabraClave();
            List<Concursante> todos = iConcursanteRepository
                    .findByConfigurarRifaIdAndPalabraClave(configurarRifaId, palabraActual);
            elegibles = todos.stream().filter(c -> !c.isDescartado()).collect(Collectors.toList());
            descartados = todos.stream().filter(Concursante::isDescartado).collect(Collectors.toList());
        }

        List<HistorialRifaVariante> historial = iHistorialRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        List<Concursante> todosConcursantes = iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);

        SorteoEstadoDto dto = new SorteoEstadoDto();
        dto.setConfigurarRifa(config);
        dto.setTotalConcursantes(todosConcursantes.size());
        dto.setTotalVariantes(variantes.size());
        dto.setVarianteNumeroActual((int) ganadoresDeclarados + (terminada ? 0 : 1));
        dto.setVarianteActual(varianteActual);
        dto.setGiroActual(giroActual);
        dto.setGiroGanador(giroGanador);
        dto.setElegibles(elegibles);
        dto.setDescartados(descartados);
        dto.setHistorial(historial);
        dto.setRifaTerminada(terminada);
        return dto;
    }

    @Transactional
    public void reiniciar(int configurarRifaId, boolean completo) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        iGanadorRifaRepository.deleteAll(iGanadorRifaRepository.findByConfigurarRifaId(configurarRifaId));
        iHistorialRifaVarianteRepository.deleteAll(
                iHistorialRifaVarianteRepository.findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId));

        if (completo) {
            iConcursanteRepository.deleteByConfigurarRifaId(configurarRifaId);
        } else {
            List<Concursante> todos = iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);
            todos.forEach(c -> c.setDescartado(false));
            iConcursanteRepository.saveAll(todos);
        }

        config.setActiva(true);
        iConfigurarRifaRepository.save(config);
        log.info("Rifa {} reiniciada. completo={}", configurarRifaId, completo);
    }
}