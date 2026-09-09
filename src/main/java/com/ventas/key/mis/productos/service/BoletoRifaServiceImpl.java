package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.models.BoletoRifaDto;
import com.ventas.key.mis.productos.models.BoletoRifaRequest;
import com.ventas.key.mis.productos.models.SorteoPlataformasDto;
import com.ventas.key.mis.productos.models.SorteoPlataformasResultadoDto;
import com.ventas.key.mis.productos.repository.IBoletoRifaRepository;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoletoRifaServiceImpl {

    private final IBoletoRifaRepository iBoletoRifaRepository;
    private final IConcursanteRepository iConcursanteRepository;
    private final IConfigurarRifaRepository iConfigurarRifaRepository;
    private final IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    private final IGanadorRifaRepository iGanadorRifaRepository;
    private final ConfigurarRifaVarianteService configurarRifaVarianteService;
    private final EmailService emailService;

    // ── Alta / baja de boletos ─────────────────────────────────────────

    @Transactional
    public BoletoRifa registrar(BoletoRifaRequest req) {
        Concursante concursante = iConcursanteRepository.findById(req.getConcursanteId())
                .orElseThrow(() -> new ExceptionDataNotFound("Concursante no encontrado"));

        // La plataforma es obligatoria: un boleto sin plataforma no dice de qué red
        // vino la acción, que es justo lo que hay que revisar al validar la rifa.
        if (req.getPlataforma() == null) {
            throw new ExceptionErrorInesperado("La plataforma del boleto es obligatoria");
        }

        LocalDate fecha = req.getFecha() != null ? req.getFecha() : LocalDate.now();
        validarFechaEnRango(concursante.getConfigurarRifa(), fecha);

        BoletoRifa boleto = new BoletoRifa();
        boleto.setConcursante(concursante);
        boleto.setPlataforma(req.getPlataforma());
        boleto.setMotivo(req.getMotivo());
        boleto.setFecha(fecha);
        boleto.setUrlPerfilRedSocial(req.getUrlPerfilRedSocial() != null
                ? req.getUrlPerfilRedSocial().trim() : null);
        boleto.setUrlSeguimiento(req.getUrlSeguimiento());
        boleto.setUrlsCompartido(req.getUrlsCompartido() != null
                ? req.getUrlsCompartido().stream()
                        .filter(u -> u != null && !u.isBlank())
                        .collect(Collectors.toList())
                : new ArrayList<>());

        BoletoRifa guardado = iBoletoRifaRepository.save(boleto);

        concursante.setBoletos(concursante.getBoletos() + 1);
        iConcursanteRepository.save(concursante);

        log.info("Boleto registrado para concursante {} (plataforma={}, motivo={}), total boletos ahora {}",
                concursante.getId(), req.getPlataforma(), req.getMotivo(), concursante.getBoletos());
        return guardado;
    }

    // Si la rifa tiene configurado un rango (fechaInicioBoletos/fechaFinBoletos), el boleto
    // solo se acepta dentro de ese rango. Si no está configurado, se usa el mes de la rifa
    // (o el mes actual) como antes, para no romper rifas que aún no definieron el rango.
    private void validarFechaEnRango(ConfigurarRifa config, LocalDate fecha) {
        LocalDate inicio = config.getFechaInicioBoletos();
        LocalDate fin = config.getFechaFinBoletos();
        if (inicio != null && fin != null) {
            if (fecha.isBefore(inicio) || fecha.isAfter(fin)) {
                throw new ExceptionErrorInesperado(
                        "La fecha del boleto debe estar entre " + inicio + " y " + fin);
            }
            validarRegistroAbierto(config);
            return;
        }

        String mesReferencia = config.getMesReferencia();
        YearMonth mesValido = (mesReferencia != null && !mesReferencia.isBlank())
                ? YearMonth.parse(mesReferencia)
                : YearMonth.now();
        if (!YearMonth.from(fecha).equals(mesValido)) {
            throw new ExceptionErrorInesperado("La fecha del boleto debe ser del mes de la rifa (" + mesValido + ")");
        }
    }

    /**
     * El periodo cierra a la hora exacta de fechaHoraLimite, no al final del último día:
     * una rifa del 1 al 9 que cierra a las 10:00 deja de recibir boletos el 9 a las 10:00,
     * no el 9 a las 23:59. Antes solo se comparaban fechas, así que la hora configurada en
     * la pantalla no tenía ningún efecto sobre el registro.
     */
    private void validarRegistroAbierto(ConfigurarRifa config) {
        LocalDateTime limite = config.getFechaHoraLimite();
        if (limite != null && LocalDateTime.now().isAfter(limite)) {
            throw new ExceptionErrorInesperado(
                    "El registro de boletos cerró el " + limite.toLocalDate()
                            + " a las " + limite.toLocalTime());
        }
    }

    public List<BoletoRifa> listarPorConcursante(Integer concursanteId) {
        return iBoletoRifaRepository.findByConcursanteId(concursanteId);
    }

    /**
     * Corrige un boleto ya registrado (se puso mal la plataforma, la fecha o una URL) sin
     * tener que eliminarlo y volverlo a capturar -- borrarlo descuenta el boleto del
     * participante y vuelve a subirlo, lo que ensucia el conteo.
     *
     * No cambia de dueño el boleto ni toca su estado de descartado: eso lo decide el sorteo.
     */
    @Transactional
    public BoletoRifa editar(Integer id, BoletoRifaRequest req) {
        BoletoRifa boleto = iBoletoRifaRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Boleto no encontrado"));

        if (req.getPlataforma() == null) {
            throw new ExceptionErrorInesperado("La plataforma del boleto es obligatoria");
        }

        LocalDate fecha = req.getFecha() != null ? req.getFecha() : boleto.getFecha();
        validarFechaEnRango(boleto.getConcursante().getConfigurarRifa(), fecha);

        boleto.setPlataforma(req.getPlataforma());
        boleto.setMotivo(req.getMotivo());
        boleto.setFecha(fecha);
        boleto.setUrlPerfilRedSocial(req.getUrlPerfilRedSocial() != null
                ? req.getUrlPerfilRedSocial().trim() : null);
        boleto.setUrlSeguimiento(req.getUrlSeguimiento());

        // Se vacía y se vuelve a llenar la MISMA lista en vez de asignar una nueva: el
        // boleto ya está gestionado por Hibernate y reemplazar la instancia de un
        // @ElementCollection le hace perder el rastro de la colección original.
        boleto.getUrlsCompartido().clear();
        if (req.getUrlsCompartido() != null) {
            boleto.getUrlsCompartido().addAll(req.getUrlsCompartido().stream()
                    .filter(u -> u != null && !u.isBlank())
                    .collect(Collectors.toList()));
        }

        BoletoRifa guardado = iBoletoRifaRepository.save(boleto);
        log.info("Boleto {} editado (plataforma={}, fecha={})", id, req.getPlataforma(), fecha);
        return guardado;
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

    // ── Sorteo de la rifa PLATAFORMAS ──────────────────────────────────
    // A diferencia del sorteo de las rifas DIARIA/MENSUAL (GanadorRifaServiceImpl),
    // aquí se sortea entre BOLETOS y el descarte quita un boleto, no a la persona:
    // quien tiene 5 boletos y pierde un giro sigue con 4 en juego. Tener más
    // boletos sigue dando más probabilidad, porque cada boleto es una entrada.

    @Transactional
    public SorteoPlataformasResultadoDto sortear(Integer rifaId) {
        return sortear(rifaId, false);
    }

    // soloPrueba=true es la llamada que viene de la página pública: cualquiera puede
    // girar mientras la rifa sea de práctica, pero la rifa real solo la gira el admin.
    @Transactional
    public SorteoPlataformasResultadoDto sortear(Integer rifaId, boolean soloPrueba) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(rifaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rifa no encontrada"));

        if (soloPrueba && !Boolean.TRUE.equals(config.getEsPrueba())) {
            throw new ExceptionErrorInesperado("La rifa real solo la puede girar el administrador");
        }

        if (!Boolean.TRUE.equals(config.getActiva())) {
            throw new ExceptionErrorInesperado("Esta rifa ya fue completada o está inactiva");
        }

        List<ConfigurarRifaVariante> variantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(rifaId);
        if (variantes.isEmpty()) {
            throw new ExceptionErrorInesperado("La rifa no tiene premios configurados");
        }

        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(rifaId);
        if (ganadoresDeclarados >= variantes.size()) {
            throw new ExceptionErrorInesperado("Todos los premios ya fueron sorteados");
        }

        ConfigurarRifaVariante varianteActual = variantes.get((int) ganadoresDeclarados);
        int giroActual = (int) iGanadorRifaRepository
                .countDescartadosByVarianteRifaId(varianteActual.getId()) + 1;

        List<BoletoRifa> enJuego = iBoletoRifaRepository.findEnJuegoByRifaId(rifaId);
        if (enJuego.isEmpty()) {
            throw new ExceptionErrorInesperado("No hay boletos en juego para esta rifa");
        }

        BoletoRifa elegido = enJuego.get(new Random().nextInt(enJuego.size()));
        boolean esGanador = giroActual >= varianteActual.getGiroGanador();
        Concursante persona = elegido.getConcursante();

        if (esGanador) {
            // Ya se llevó el premio: salen todos sus boletos para que no gane el siguiente
            List<BoletoRifa> suyos = enJuego.stream()
                    .filter(b -> b.getConcursante().getId().equals(persona.getId()))
                    .collect(Collectors.toList());
            suyos.forEach(b -> b.setDescartado(true));
            iBoletoRifaRepository.saveAll(suyos);
        } else {
            // Solo se descarta ESE boleto -- la persona sigue con los demás
            elegido.setDescartado(true);
            iBoletoRifaRepository.save(elegido);
        }

        GanadorRifa gr = new GanadorRifa();
        gr.setConcursante(persona);
        gr.setConfigurarRifaVariante(varianteActual);
        gr.setDescartado(!esGanador);
        iGanadorRifaRepository.save(gr);

        boolean esUltimaVariante = ganadoresDeclarados + 1 >= variantes.size();
        boolean rifaTerminada = esGanador && esUltimaVariante;
        boolean esReal = !Boolean.TRUE.equals(config.getEsPrueba());

        if (esGanador && esReal) {
            // Consumir la reserva de stock: el premio ya se entregó, así que si después
            // se quita la variante de la rifa el stock NO se devuelve.
            varianteActual.setStockReservado(0);
            iConfigurarRifaVarianteRepository.save(varianteActual);

            if (persona.getCorreo() != null && !persona.getCorreo().isBlank()) {
                String premio = varianteActual.getVariante().getProducto().getNombre();
                emailService.enviarNotificacionGanador(persona.getCorreo(), persona.getNombre(), premio);
            }
        }

        if (rifaTerminada && esReal) {
            config.setActiva(false);
            iConfigurarRifaRepository.save(config);
            log.info("Rifa PLATAFORMAS {} completada", rifaId);
        }

        log.info("Sorteo plataformas rifa={} boleto={} concursante={} giro={}/{} esGanador={}",
                rifaId, elegido.getId(), persona.getId(), giroActual, varianteActual.getGiroGanador(), esGanador);

        SorteoPlataformasResultadoDto resultado = new SorteoPlataformasResultadoDto();
        resultado.setBoleto(soloPrueba ? toDtoPublico(elegido) : toDto(elegido));
        resultado.setEsGanador(esGanador);
        resultado.setVarianteActual(configurarRifaVarianteService.toDto(varianteActual));
        resultado.setGiroActual(giroActual);
        resultado.setGiroGanador(varianteActual.getGiroGanador());
        resultado.setRifaTerminada(rifaTerminada);
        return resultado;
    }

    public SorteoPlataformasDto obtenerEstado(Integer rifaId) {
        return obtenerEstado(rifaId, false);
    }

    // publico=true recorta lo que no debe salir de la pantalla del admin: las URLs de
    // evidencia de cada boleto y los datos de contacto que cuelgan de los ganadores.
    public SorteoPlataformasDto obtenerEstado(Integer rifaId, boolean publico) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(rifaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rifa no encontrada"));

        List<ConfigurarRifaVariante> variantes = iConfigurarRifaVarianteRepository
                .findByConfigurarRifaIdOrderByOrdenAsc(rifaId);
        long ganadoresDeclarados = iGanadorRifaRepository.countGanadoresByRifaId(rifaId);
        boolean terminada = !variantes.isEmpty() && ganadoresDeclarados >= variantes.size();

        ConfigurarRifaVariante varianteActual = null;
        int giroActual = 0;
        int giroGanador = 0;
        if (!terminada && !variantes.isEmpty()) {
            varianteActual = variantes.get((int) ganadoresDeclarados);
            giroActual = (int) iGanadorRifaRepository
                    .countDescartadosByVarianteRifaId(varianteActual.getId()) + 1;
            giroGanador = varianteActual.getGiroGanador();
        }

        List<BoletoRifa> todos = iBoletoRifaRepository.findByRifaId(rifaId);

        SorteoPlataformasDto dto = new SorteoPlataformasDto();
        dto.setConfigurarRifa(config);
        dto.setVariantes(variantes.stream()
                .map(configurarRifaVarianteService::toDto).collect(Collectors.toList()));
        dto.setVarianteActual(varianteActual != null ? configurarRifaVarianteService.toDto(varianteActual) : null);
        dto.setVarianteNumeroActual((int) ganadoresDeclarados + (terminada ? 0 : 1));
        dto.setTotalVariantes(variantes.size());
        dto.setGiroActual(giroActual);
        dto.setGiroGanador(giroGanador);
        dto.setBoletosEnJuego(todos.stream().filter(b -> !b.isDescartado())
                .map(b -> publico ? toDtoPublico(b) : toDto(b)).collect(Collectors.toList()));
        dto.setBoletosDescartados(todos.stream().filter(BoletoRifa::isDescartado)
                .map(b -> publico ? toDtoPublico(b) : toDto(b)).collect(Collectors.toList()));
        dto.setGanadores(publico ? List.of() : iGanadorRifaRepository.findGanadoresByRifaId(rifaId));
        dto.setRifaTerminada(terminada);
        return dto;
    }

    // Deja la rifa como estaba antes de girar: se borran los giros y todos los
    // boletos vuelven a estar en juego.
    @Transactional
    public void reiniciar(Integer rifaId) {
        reiniciar(rifaId, false);
    }

    @Transactional
    public void reiniciar(Integer rifaId, boolean soloPrueba) {
        ConfigurarRifa config = iConfigurarRifaRepository.findById(rifaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rifa no encontrada"));

        if (soloPrueba && !Boolean.TRUE.equals(config.getEsPrueba())) {
            throw new ExceptionErrorInesperado("La rifa real solo la puede reiniciar el administrador");
        }

        iGanadorRifaRepository.deleteByRifaId(rifaId);
        iBoletoRifaRepository.reactivarTodosPorRifa(rifaId);

        config.setActiva(true);
        iConfigurarRifaRepository.save(config);
        log.info("Rifa PLATAFORMAS {} reiniciada -- todos los boletos vuelven a estar en juego", rifaId);
    }

    // Versión para la página pública: se ve quién es y qué hizo, pero NUNCA las URLs
    // de evidencia (perfil, seguimiento, publicaciones) -- eso es solo del admin.
    public BoletoRifaDto toDtoPublico(BoletoRifa b) {
        BoletoRifaDto dto = toDto(b);
        dto.setUrlPerfilRedSocial(null);
        dto.setUrlSeguimiento(null);
        dto.setUrlsCompartido(null);
        return dto;
    }

    public BoletoRifaDto toDto(BoletoRifa b) {
        BoletoRifaDto dto = new BoletoRifaDto();
        dto.setId(b.getId());
        dto.setPlataforma(b.getPlataforma() != null ? b.getPlataforma().name() : null);
        dto.setMotivo(b.getMotivo());
        dto.setFecha(b.getFecha());
        dto.setUrlPerfilRedSocial(b.getUrlPerfilRedSocial());
        dto.setUrlSeguimiento(b.getUrlSeguimiento());
        dto.setUrlsCompartido(b.getUrlsCompartido());
        dto.setDescartado(b.isDescartado());

        Concursante c = b.getConcursante();
        if (c != null) {
            dto.setConcursanteId(c.getId());
            dto.setNombreCompleto(
                    (c.getNombre() != null ? c.getNombre() : "")
                    + (c.getApellidoPaterno() != null && !c.getApellidoPaterno().isBlank()
                        ? " " + c.getApellidoPaterno() : ""));
        }
        return dto;
    }
}
