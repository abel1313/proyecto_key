package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.SorteoEstadoDto;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;

@Service
public class GanadorRifaServiceImpl extends CrudAbstractServiceImpl<GanadorRifa, List<GanadorRifa>, Optional<GanadorRifa>, Integer, PginaDto<List<GanadorRifa>>> {

    private final IGanadorRifaRepository iGanadorRifaRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;
    private final IConcursanteRepository iConcursanteRepository;
    private final IProductosRepository iProductosRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GanadorRifaServiceImpl(
            final IGanadorRifaRepository iGanadorRifaRepository,
            final ErrorGenerico eGenerico,
            final IConfigurarRifaRepository iConfigurarRifaRepository,
            final IConcursanteRepository iConcursanteRepository,
            final IProductosRepository iProductosRepository,
            final SimpMessagingTemplate messagingTemplate) {
        super(iGanadorRifaRepository, eGenerico);
        this.iGanadorRifaRepository = iGanadorRifaRepository;
        this.iConfigurarRifaRepository = iConfigurarRifaRepository;
        this.iConcursanteRepository = iConcursanteRepository;
        this.iProductosRepository = iProductosRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public GanadorRifa sortear(int configurarRifaId, int vueltaActual, int totalVueltas) {
        if (vueltaActual < 1 || vueltaActual > totalVueltas) {
            throw new RuntimeException("Vuelta inválida: " + vueltaActual + " de " + totalVueltas);
        }

        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new RuntimeException("Esta rifa ya fue sorteada o está inactiva");
        }

        List<Concursante> elegibles = iConcursanteRepository.findByConfigurarRifaIdAndDescartadoFalse(configurarRifaId);
        if (elegibles.isEmpty()) {
            throw new RuntimeException("No hay concursantes elegibles en esta rifa");
        }

        Concursante seleccionado = elegibles.get(new Random().nextInt(elegibles.size()));

        boolean esVueltaFinal = vueltaActual == totalVueltas;

        if (!esVueltaFinal) {
            seleccionado.setDescartado(true);
            iConcursanteRepository.save(seleccionado);

            GanadorRifa descartado = new GanadorRifa();
            descartado.setConcursante(seleccionado);
            descartado.setProducto(config.getProducto());
            descartado.setDescartado(true);
            messagingTemplate.convertAndSend("/topic/ruleta", descartado);
            return descartado;
        }

        Producto producto = iProductosRepository.findById(config.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStock() < 1) {
            throw new RuntimeException("Sin stock disponible para el premio");
        }
        producto.setStock(producto.getStock() - 1);
        iProductosRepository.save(producto);

        GanadorRifa ganador = new GanadorRifa();
        ganador.setConcursante(seleccionado);
        ganador.setProducto(producto);
        ganador.setDescartado(false);
        GanadorRifa ganadorGuardado = iGanadorRifaRepository.save(ganador);

        config.setActiva(false);
        iConfigurarRifaRepository.save(config);

        messagingTemplate.convertAndSend("/topic/ruleta", ganadorGuardado);
        return ganadorGuardado;
    }

    public SorteoEstadoDto obtenerEstado(int configurarRifaId) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        List<Concursante> todos = iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);
        List<Concursante> elegibles = todos.stream().filter(c -> !c.isDescartado()).collect(Collectors.toList());
        List<Concursante> descartados = todos.stream().filter(Concursante::isDescartado).collect(Collectors.toList());

        GanadorRifa ganador = iGanadorRifaRepository
                .findByConcursanteConfigurarRifaIdAndDescartadoFalse(configurarRifaId)
                .orElse(null);

        int vueltaActual = descartados.size() + 1;

        return new SorteoEstadoDto(config, todos.size(), vueltaActual, elegibles, descartados, ganador);
    }

    @Transactional
    public void reiniciar(int configurarRifaId, boolean completo) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        List<GanadorRifa> ganadoresExistentes = iGanadorRifaRepository
                .findByConcursanteConfigurarRifaId(configurarRifaId);

        for (GanadorRifa g : ganadoresExistentes) {
            if (!g.isDescartado()) {
                Producto producto = iProductosRepository.findById(g.getProducto().getId())
                        .orElse(null);
                if (producto != null) {
                    producto.setStock(producto.getStock() + 1);
                    iProductosRepository.save(producto);
                }
            }
        }
        iGanadorRifaRepository.deleteAll(ganadoresExistentes);

        if (completo) {
            iConcursanteRepository.deleteByConfigurarRifaId(configurarRifaId);
        } else {
            List<Concursante> todos = iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);
            todos.forEach(c -> c.setDescartado(false));
            iConcursanteRepository.saveAll(todos);
        }

        config.setActiva(true);
        iConfigurarRifaRepository.save(config);
    }
}