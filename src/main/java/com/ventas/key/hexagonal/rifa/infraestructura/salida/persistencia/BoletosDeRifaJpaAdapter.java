package com.ventas.key.hexagonal.rifa.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion;
import com.ventas.key.hexagonal.rifa.dominio.modelo.PerfilEnRed;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;
import com.ventas.key.hexagonal.rifa.dominio.puerto.salida.BoletosDeRifaPort;
import com.ventas.key.mis.productos.entity.BoletoRifa;
import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.repository.IBoletoRifaRepository;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Traduce entre las filas de {@code boletos_rifa} y los grupos del dominio.
 *
 * <p>El agrupamiento pasa <b>aca</b>, al leer: en la base cada participacion sigue siendo su
 * propia fila (decision D4), porque el sorteo elige filas al azar y juntarlas le quitaria
 * chances a la persona.</p>
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Interface Adapter / Gateway]</p>
 */
@Component
public class BoletosDeRifaJpaAdapter implements BoletosDeRifaPort {

    private final IBoletoRifaRepository iBoletoRifaRepository;
    private final IConcursanteRepository iConcursanteRepository;

    public BoletosDeRifaJpaAdapter(IBoletoRifaRepository iBoletoRifaRepository,
            IConcursanteRepository iConcursanteRepository) {
        this.iBoletoRifaRepository = iBoletoRifaRepository;
        this.iConcursanteRepository = iConcursanteRepository;
    }

    @Override
    public List<GrupoDeBoletos> gruposDeLaRifa(Integer rifaId) {
        return agrupar(iBoletoRifaRepository.findByRifaId(rifaId));
    }

    @Override
    public Optional<GrupoDeBoletos> buscarGrupo(Integer rifaId, PerfilEnRed perfil) {
        return gruposDeLaRifa(rifaId).stream()
                .filter(g -> g.perfil().mismoQue(perfil))
                .findFirst();
    }

    /**
     * Una fila por participacion. La {@code @ElementCollection urlsCompartido} se deja
     * vacia a proposito en el formato nuevo: las URLs que viven ahi no suman boletos,
     * porque el sorteo cuenta filas.
     */
    @Override
    public Participacion crearParticipacion(Integer concursanteId, PerfilEnRed perfil,
            Participacion nueva) {
        Concursante concursante = iConcursanteRepository.getReferenceById(concursanteId);

        BoletoRifa fila = new BoletoRifa();
        fila.setConcursante(concursante);
        fila.setPlataforma(aPlataformaEntidad(perfil.plataforma()));
        fila.setUrlPerfilRedSocial(perfil.urlPerfil());
        fila.setUrlSeguimiento(nueva.urlParticipacion());
        fila.setMotivo(nueva.motivo());
        fila.setFecha(nueva.fecha() != null ? nueva.fecha() : LocalDate.now());
        fila.setDescartado(false);

        BoletoRifa guardada = iBoletoRifaRepository.save(fila);
        return aParticipacion(guardada);
    }

    @Override
    public void actualizarParticipacion(Integer boletoId, String urlParticipacion, String motivo) {
        BoletoRifa fila = iBoletoRifaRepository.findById(boletoId).orElseThrow();
        fila.setUrlSeguimiento(urlParticipacion);
        fila.setMotivo(motivo);
        iBoletoRifaRepository.save(fila);
    }

    @Override
    public void borrarParticipacion(Integer boletoId) {
        iBoletoRifaRepository.deleteById(boletoId);
    }

    @Override
    public Optional<String> nombreDelConcursante(Integer concursanteId) {
        return iConcursanteRepository.findById(concursanteId).map(this::nombreDe);
    }

    /** Agrupa por (plataforma + perfil normalizado), conservando el orden de llegada. */
    private List<GrupoDeBoletos> agrupar(List<BoletoRifa> filas) {
        Map<String, List<BoletoRifa>> porPerfil = new LinkedHashMap<>();
        for (BoletoRifa fila : filas) {
            porPerfil.computeIfAbsent(claveDe(fila), k -> new ArrayList<>()).add(fila);
        }

        List<GrupoDeBoletos> grupos = new ArrayList<>();
        for (List<BoletoRifa> delMismoPerfil : porPerfil.values()) {
            BoletoRifa primera = delMismoPerfil.get(0);
            List<Participacion> participaciones = delMismoPerfil.stream()
                    .map(this::aParticipacion)
                    .toList();
            grupos.add(new GrupoDeBoletos(primera.getConcursante().getId(),
                    nombreDe(primera.getConcursante()),
                    new PerfilEnRed(aPlataformaDominio(primera.getPlataforma()),
                            primera.getUrlPerfilRedSocial()),
                    participaciones));
        }
        return masRecienteArriba(grupos);
    }

    /**
     * Lo ultimo que se cargo va primero.
     *
     * <p>Con el orden de insercion, el grupo que el admin acaba de cargar queda al final de
     * la lista: hay que bajar hasta abajo para verlo y volver a subir para cargar el
     * siguiente. Poniendo arriba lo mas reciente, lo que se acaba de tocar queda a la vista
     * sin moverse.</p>
     *
     * <p>Los grupos sin participaciones (quedaron vacios al quitarles la ultima) van al
     * final: no hay nada que revisar ahi.</p>
     */
    private List<GrupoDeBoletos> masRecienteArriba(List<GrupoDeBoletos> grupos) {
        return grupos.stream()
                .sorted(Comparator.comparing(GrupoDeBoletos::ultimaParticipacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * La clave incluye al concursante ademas del perfil: dos clientes distintos que pegaron
     * la misma url de perfil son dos grupos, no uno. Pasa cuando se carga mal el perfil.
     */
    private String claveDe(BoletoRifa fila) {
        return fila.getConcursante().getId() + "#"
                + aPlataformaDominio(fila.getPlataforma()).name() + "|"
                + PerfilEnRed.normalizar(fila.getUrlPerfilRedSocial());
    }

    /**
     * Los boletos cargados con la pantalla vieja guardaban la publicacion en
     * {@code urlsCompartido}, no en {@code urlSeguimiento}: sin esto salian "sin link".
     */
    private Participacion aParticipacion(BoletoRifa fila) {
        String url = fila.getUrlSeguimiento();
        if ((url == null || url.isBlank()) && fila.getUrlsCompartido() != null) {
            url = fila.getUrlsCompartido().stream()
                    .filter(u -> u != null && !u.isBlank())
                    .findFirst()
                    .orElse(url);
        }
        return new Participacion(fila.getId(), url, fila.getMotivo(), fila.getFecha());
    }

    private String nombreDe(Concursante c) {
        String apellido = c.getApellidoPaterno();
        return apellido == null || apellido.isBlank() ? c.getNombre() : c.getNombre() + " " + apellido;
    }

    private BoletoRifa.Plataforma aPlataformaEntidad(Plataforma plataforma) {
        return plataforma == null ? BoletoRifa.Plataforma.OTRO
                : BoletoRifa.Plataforma.valueOf(plataforma.name());
    }

    private Plataforma aPlataformaDominio(BoletoRifa.Plataforma plataforma) {
        return plataforma == null ? Plataforma.OTRO : Plataforma.valueOf(plataforma.name());
    }
}
