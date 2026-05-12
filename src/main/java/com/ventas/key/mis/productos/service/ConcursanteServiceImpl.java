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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    private int[] calcularBoletos(Integer clientePedidoId, boolean sinRegistro, String mes) {
        List<Object[]> resultado = iPedidoRepository.calcularScore(clientePedidoId, sinRegistro, mes);
        if (resultado == null || resultado.isEmpty()) return new int[]{1, 1};

        Object[] row = resultado.get(0);
        long cumplimientos   = row[0] != null ? ((Number) row[0]).longValue() : 0;
        long incumplimientos = row[1] != null ? ((Number) row[1]).longValue() : 0;
        long comprasMes      = row[2] != null ? ((Number) row[2]).longValue() : 0;

        double scoreValor = (cumplimientos + incumplimientos == 0) ? 1.0
                : (double) cumplimientos / (cumplimientos + incumplimientos);

        int base     = (int) Math.max(1, comprasMes);
        int efectivo = Math.max(1, (int) Math.round(comprasMes * scoreValor));
        return new int[]{base, efectivo};
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

        if (concursante.getClientePedidoId() != null) {
            String mesActual = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            int[] boletos = calcularBoletos(concursante.getClientePedidoId(), false, mesActual);
            concursante.setBoletosBase(boletos[0]);
            concursante.setBoletos(boletos[1]);
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
            dto.setSinRegistro(row[3] != null && ((Number) row[3]).intValue() == 1);
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

        String mes = req.getMes();
        List<Concursante> importados = new ArrayList<>();

        for (ImportarDePedidosRequest.ClientePedidoDto cliente : req.getClientes()) {
            int boletosBase = 1;
            int boletos     = 1;

            if (mes != null && !mes.isBlank() && cliente.getClientePedidoId() != null) {
                int[] calculado = calcularBoletos(cliente.getClientePedidoId(), cliente.isSinRegistro(), mes);
                boletosBase = calculado[0];
                boletos     = calculado[1];
            }

            Concursante c = new Concursante();
            c.setNombre(cliente.getNombre());
            c.setApellidoPaterno(cliente.getApellidoPaterno());
            c.setTelefono(cliente.getTelefono());
            c.setPalabraClave(req.getPalabraClave());
            c.setOrdenDesde(req.getOrdenDesde());
            c.setClientePedidoId(cliente.getClientePedidoId());
            c.setConfigurarRifa(config);
            c.setBoletosBase(boletosBase);
            c.setBoletos(boletos);
            importados.add(iConcursanteRepository.save(c));
        }
        return importados;
    }
}