package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.models.BoletoRifaRequest;
import com.ventas.key.mis.productos.repository.IBoletoRifaRepository;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoletoRifaServiceImpl {

    private final IBoletoRifaRepository iBoletoRifaRepository;
    private final IConcursanteRepository iConcursanteRepository;

    @Transactional
    public BoletoRifa registrar(BoletoRifaRequest req) {
        Concursante concursante = iConcursanteRepository.findById(req.getConcursanteId())
                .orElseThrow(() -> new ExceptionDataNotFound("Concursante no encontrado"));

        LocalDate fecha = req.getFecha() != null ? req.getFecha() : LocalDate.now();
        String mesReferencia = concursante.getConfigurarRifa().getMesReferencia();
        YearMonth mesValido = (mesReferencia != null && !mesReferencia.isBlank())
                ? YearMonth.parse(mesReferencia)
                : YearMonth.now();
        if (!YearMonth.from(fecha).equals(mesValido)) {
            throw new ExceptionErrorInesperado("La fecha del boleto debe ser del mes de la rifa (" + mesValido + ")");
        }

        BoletoRifa boleto = new BoletoRifa();
        boleto.setConcursante(concursante);
        boleto.setMotivo(req.getMotivo());
        boleto.setFecha(fecha);
        boleto.setUrlPerfilRedSocial(req.getUrlPerfilRedSocial());
        boleto.setUrlSeguimiento(req.getUrlSeguimiento());
        boleto.setUrlsCompartido(req.getUrlsCompartido() != null
                ? req.getUrlsCompartido().stream()
                        .filter(u -> u != null && !u.isBlank())
                        .collect(Collectors.toList())
                : new ArrayList<>());

        BoletoRifa guardado = iBoletoRifaRepository.save(boleto);

        concursante.setBoletos(concursante.getBoletos() + 1);
        iConcursanteRepository.save(concursante);

        log.info("Boleto registrado para concursante {} (motivo={}), total boletos ahora {}",
                concursante.getId(), req.getMotivo(), concursante.getBoletos());
        return guardado;
    }

    public List<BoletoRifa> listarPorConcursante(Integer concursanteId) {
        return iBoletoRifaRepository.findByConcursanteId(concursanteId);
    }

    @Transactional
    public void eliminar(Integer id) {
        BoletoRifa boleto = iBoletoRifaRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Boleto no encontrado"));

        Concursante concursante = boleto.getConcursante();
        int nuevoTotal = Math.max(concursante.getBoletosBase(), concursante.getBoletos() - 1);
        concursante.setBoletos(nuevoTotal);
        iConcursanteRepository.save(concursante);

        iBoletoRifaRepository.delete(boleto);
        log.info("Boleto {} eliminado, concursante {} vuelve a {} boletos", id, concursante.getId(), nuevoTotal);
    }
}
