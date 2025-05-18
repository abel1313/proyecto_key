package com.ventas.key.mis.productos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IRifaRepository;
import com.ventas.key.mis.productos.service.api.IRifaService;

@Service
public class RifaServiceImpl extends CrudAbstractServiceImpl< Rifa,List<Rifa>,Optional<Rifa>, Integer, PginaDto<List<Rifa>>>
implements IRifaService {

    private final IRifaRepository iRifaRepository;
    private final ErrorGenerico errorGenerico;
    public RifaServiceImpl(
        final IRifaRepository iRifaRepository,
        final ErrorGenerico eGenerico
    ){
        super(iRifaRepository, eGenerico);
        this.iRifaRepository = iRifaRepository;
        this.errorGenerico = eGenerico;
    }
    @Override
    public List<Rifa> buscarPorRangoDeHora(String inicio, String fin, String palabraRifa)throws Exception{
        if(!validarRangoHoras(inicio, fin) ){
            throw new Exception("La hora inicio es menor a la hora fin");
        }
        LocalDate hoy = LocalDate.now();
        LocalDateTime horaInicioRifa = hoy.atTime(LocalTime.parse(inicio));
        LocalDateTime horaRifaFin = hoy.atTime(LocalTime.parse(fin));
        return this.iRifaRepository.buscarPorRangoDeHora(horaInicioRifa,horaRifaFin, palabraRifa);
    }

        private boolean validarRangoHoras(String inicioStr, String finStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime inicio = LocalTime.parse(inicioStr, formatter);
        LocalTime fin = LocalTime.parse(finStr, formatter);

        return !inicio.isAfter(fin);
    }


}
