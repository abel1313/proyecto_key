package com.ventas.key.mis.productos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IRifaRepository;
import com.ventas.key.mis.productos.service.api.IRifaService;

@Service
public class RifaServiceImpl extends CrudAbstractServiceImpl<Rifa, List<Rifa>, Optional<Rifa>, Integer, PginaDto<List<Rifa>>>
        implements IRifaService {

    private final IRifaRepository iRifaRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;

    public RifaServiceImpl(
            final IRifaRepository iRifaRepository,
            final IConfigurarRifaRepository iConfigurarRifaRepository,
            final ErrorGenerico eGenerico) {
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.iConfigurarRifaRepository = iConfigurarRifaRepository;
    }

    @Override
    public Rifa registrar(Rifa rifa, boolean forzar) throws Exception {
        if (rifa.getConfigurarRifa() == null || rifa.getConfigurarRifa().getId() == null) {
            throw new Exception("Debe indicar la configuración de rifa");
        }
        ConfigurarRifa config = iConfigurarRifaRepository.findById(rifa.getConfigurarRifa().getId())
                .orElseThrow(() -> new Exception("Configuración de rifa no encontrada"));

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new Exception("Esta rifa ya fue sorteada o está inactiva");
        }
        if (!forzar && LocalDateTime.now().isAfter(config.getFechaHoraLimite())) {
            throw new Exception("El plazo de registro ha cerrado el " + config.getFechaHoraLimite());
        }
        return save(rifa);
    }

    @Override
    public List<Rifa> buscarPorConfiguracion(Integer configurarRifaId) {
        return iRifaRepository.findByConfigurarRifaId(configurarRifaId);
    }

    @Override
    public List<Rifa> buscarPorRangoDeHora(String inicio, String fin, String palabraRifa) throws Exception {
        if (!validarRangoHoras(inicio, fin)) {
            throw new Exception("La hora inicio es mayor a la hora fin");
        }
        LocalDate hoy = LocalDate.now();
        LocalDateTime horaInicioRifa = hoy.atTime(LocalTime.parse(inicio));
        LocalDateTime horaRifaFin = hoy.atTime(LocalTime.parse(fin));
        return iRifaRepository.buscarPorRangoDeHora(horaInicioRifa, horaRifaFin, palabraRifa);
    }

    private boolean validarRangoHoras(String inicioStr, String finStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime inicio = LocalTime.parse(inicioStr, formatter);
        LocalTime fin = LocalTime.parse(finStr, formatter);
        return !inicio.isAfter(fin);
    }
}
