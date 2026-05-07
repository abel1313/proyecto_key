package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaProducto;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.SorteoEstadoDto;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaProductoRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
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
    private final IConfigurarRifaProductoRepository iConfigurarRifaProductoRepository;
    private final IConcursanteRepository iConcursanteRepository;
    private final IProductosRepository iProductosRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GanadorRifaServiceImpl(
            final IGanadorRifaRepository iGanadorRifaRepository,
            final ErrorGenerico eGenerico,
            final IConfigurarRifaRepository iConfigurarRifaRepository,
            final IConfigurarRifaProductoRepository iConfigurarRifaProductoRepository,
            final IConcursanteRepository iConcursanteRepository,
            final IProductosRepository iProductosRepository,
            final SimpMessagingTemplate messagingTemplate) {
        super(iGanadorRifaRepository, eGenerico);
        this.iGanadorRifaRepository = iGanadorRifaRepository;
        this.iConfigurarRifaRepository = iConfigurarRifaRepository;
        this.iConfigurarRifaProductoRepository = iConfigurarRifaProductoRepository;
        this.iConcursanteRepository = iConcursanteRepository;
        this.iProductosRepository = iProductosRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public GanadorRifa sortear(int configurarRifaId) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new RuntimeException("Esta rifa ya fue completada o está inactiva");
        }

        List<ConfigurarRifaProducto> productos = iConfigurarRifaProductoRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        if (productos.isEmpty()) {
            throw new RuntimeException("La rifa no tiene productos configurados");
        }

        // Producto actual = número de ganadores ya declarados
        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(configurarRifaId);
        if (ganadoresDeclarados >= productos.size()) {
            throw new RuntimeException("Todos los productos ya fueron rifados");
        }

        ConfigurarRifaProducto productoActual = productos.get((int) ganadoresDeclarados);
        long girosHechos = iGanadorRifaRepository.countDescartadosByProductoRifaId(productoActual.getId());
        long giroActual = girosHechos + 1;

        List<Concursante> elegibles = iConcursanteRepository
                .findByConfigurarRifaIdAndDescartadoFalseAndOrdenDesdeLessThanEqual(
                        configurarRifaId, productoActual.getOrden());

        if (elegibles.isEmpty()) {
            throw new RuntimeException("No hay concursantes elegibles para el producto " + productoActual.getOrden());
        }

        Concursante seleccionado = elegibles.get(new Random().nextInt(elegibles.size()));
        boolean esGanador = giroActual >= productoActual.getGiroGanador();

        GanadorRifa gr = new GanadorRifa();
        gr.setConcursante(seleccionado);
        gr.setConfigurarRifaProducto(productoActual);
        gr.setDescartado(!esGanador);

        if (esGanador) {
            Producto producto = iProductosRepository.findById(productoActual.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            if (producto.getStock() < 1) {
                throw new RuntimeException("Sin stock disponible para el premio");
            }
            producto.setStock(producto.getStock() - 1);
            iProductosRepository.save(producto);

            seleccionado.setDescartado(true);
            iConcursanteRepository.save(seleccionado);

            if (ganadoresDeclarados + 1 >= productos.size()) {
                config.setActiva(false);
                iConfigurarRifaRepository.save(config);
                log.info("Rifa {} completada", configurarRifaId);
            }
        } else {
            seleccionado.setDescartado(true);
            iConcursanteRepository.save(seleccionado);
        }

        GanadorRifa guardado = iGanadorRifaRepository.save(gr);
        messagingTemplate.convertAndSend("/topic/ruleta", guardado);
        log.info("Sorteo rifa={} producto={} giro={}/{} esGanador={}",
                configurarRifaId, productoActual.getOrden(), giroActual,
                productoActual.getGiroGanador(), esGanador);
        return guardado;
    }

    public SorteoEstadoDto obtenerEstado(int configurarRifaId) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(configurarRifaId)
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        List<ConfigurarRifaProducto> productos = iConfigurarRifaProductoRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(configurarRifaId);

        List<Concursante> todos = iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);
        List<Concursante> elegibles = todos.stream().filter(c -> !c.isDescartado()).collect(Collectors.toList());
        List<Concursante> descartados = todos.stream().filter(Concursante::isDescartado).collect(Collectors.toList());
        List<GanadorRifa> historial = iGanadorRifaRepository.findByConfigurarRifaId(configurarRifaId);

        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(configurarRifaId);
        boolean terminada = !productos.isEmpty() && ganadoresDeclarados >= productos.size();

        ConfigurarRifaProducto productoActual = null;
        int giroActual = 0;
        int giroGanador = 0;

        if (!terminada && !productos.isEmpty()) {
            productoActual = productos.get((int) ganadoresDeclarados);
            giroActual = (int) iGanadorRifaRepository.countDescartadosByProductoRifaId(productoActual.getId()) + 1;
            giroGanador = productoActual.getGiroGanador();
        }

        SorteoEstadoDto dto = new SorteoEstadoDto();
        dto.setConfigurarRifa(config);
        dto.setTotalConcursantes(todos.size());
        dto.setProductoActual(productoActual);
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
                .orElseThrow(() -> new RuntimeException("Configuración de rifa no encontrada"));

        List<GanadorRifa> ganadoresExistentes = iGanadorRifaRepository.findAllGanadoresByRifaId(configurarRifaId);
        for (GanadorRifa g : ganadoresExistentes) {
            Producto producto = iProductosRepository.findById(
                    g.getConfigurarRifaProducto().getProducto().getId()).orElse(null);
            if (producto != null) {
                producto.setStock(producto.getStock() + 1);
                iProductosRepository.save(producto);
            }
        }

        List<GanadorRifa> todoHistorial = iGanadorRifaRepository.findByConfigurarRifaId(configurarRifaId);
        iGanadorRifaRepository.deleteAll(todoHistorial);

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