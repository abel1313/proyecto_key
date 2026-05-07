package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ClientePedidoDto;
import com.ventas.key.mis.productos.models.ImportarDePedidosRequest;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.service.api.IConcursanteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConcursanteServiceImpl extends CrudAbstractServiceImpl<
        Concursante,
        List<Concursante>,
        Optional<Concursante>,
        Integer,
        PginaDto<List<Concursante>>>
        implements IConcursanteService {

    private final IConcursanteRepository iConcursanteRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;
    private final IPedidoRepository iPedidoRepository;

    public ConcursanteServiceImpl(
            final IConcursanteRepository iConcursanteRepository,
            final IConfigurarRifaRepository iConfigurarRifaRepository,
            final IPedidoRepository iPedidoRepository,
            final ErrorGenerico eGenerico) {
        super(iConcursanteRepository, eGenerico);
        this.iConcursanteRepository = iConcursanteRepository;
        this.iConfigurarRifaRepository = iConfigurarRifaRepository;
        this.iPedidoRepository = iPedidoRepository;
    }

    @Override
    public Concursante registrar(Concursante concursante, boolean forzar) throws Exception {
        if (concursante.getConfigurarRifa() == null || concursante.getConfigurarRifa().getId() == null) {
            throw new Exception("Debe indicar la configuración de rifa");
        }
        ConfigurarRifa config = iConfigurarRifaRepository.findById(concursante.getConfigurarRifa().getId())
                .orElseThrow(() -> new Exception("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new Exception("Esta rifa ya fue sorteada o está inactiva");
        }
        if (!forzar && LocalDateTime.now().isAfter(config.getFechaHoraLimite())) {
            throw new Exception("El plazo de registro cerró el " + config.getFechaHoraLimite());
        }
        concursante.setConfigurarRifa(config);
        return save(concursante);
    }

    @Override
    public List<Concursante> buscarPorConfiguracion(Integer configurarRifaId) {
        return iConcursanteRepository.findByConfigurarRifaId(configurarRifaId);
    }

    @Override
    public List<Concursante> buscarElegibles(Integer configurarRifaId) {
        return iConcursanteRepository.findByConfigurarRifaIdAndDescartadoFalse(configurarRifaId);
    }

    @Override
    public Concursante descartar(Integer concursanteId) throws Exception {
        Concursante concursante = iConcursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new Exception("Concursante no encontrado"));
        concursante.setDescartado(true);
        return iConcursanteRepository.save(concursante);
    }

    public List<ClientePedidoDto> clientesPorMes(String mes) {
        List<Object[]> rows = iPedidoRepository.findClientesUnicosPorMes(mes);
        List<ClientePedidoDto> resultado = new ArrayList<>();
        for (Object[] row : rows) {
            ClientePedidoDto dto = new ClientePedidoDto();
            dto.setClientePedidoId(row[0] != null ? ((Number) row[0]).intValue() : null);
            dto.setNombre(row[1] != null ? row[1].toString() : "");
            dto.setTelefono(row[2] != null ? row[2].toString() : "");
            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional
    public List<Concursante> importarDePedidos(ImportarDePedidosRequest req) throws Exception {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(req.getConfigurarRifaId())
                .orElseThrow(() -> new Exception("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new Exception("Esta rifa no está activa");
        }

        List<Concursante> importados = new ArrayList<>();
        for (ImportarDePedidosRequest.ClientePedidoDto cliente : req.getClientes()) {
            Concursante c = new Concursante();
            c.setNombre(cliente.getNombre());
            c.setApellidoPaterno(cliente.getApellidoPaterno());
            c.setTelefono(cliente.getTelefono());
            c.setPalabraClave(req.getPalabraClave());
            c.setOrdenDesde(req.getOrdenDesde());
            c.setClientePedidoId(cliente.getClientePedidoId());
            c.setConfigurarRifa(config);
            importados.add(iConcursanteRepository.save(c));
        }
        return importados;
    }
}