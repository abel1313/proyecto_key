package com.ventas.key.hexagonal.rifa.aplicacion.servicio;

import com.ventas.key.hexagonal.rifa.dominio.excepcion.CargaSinParticipacionesException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.UrlParticipacionDuplicadaException;
import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.ModoDeCarga;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion;
import com.ventas.key.hexagonal.rifa.dominio.modelo.PerfilEnRed;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;
import com.ventas.key.hexagonal.rifa.dominio.puerto.entrada.GestionarBoletosCasoUso;
import com.ventas.key.hexagonal.rifa.dominio.puerto.salida.BoletosDeRifaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carga de boletos por participacion en redes, agrupada por perfil.
 *
 * <p>Las reglas estan en {@code hexagonal/rifa/README.md}. Las dos que mas condicionan este
 * codigo:</p>
 * <ul>
 *   <li><b>D4</b> — una participacion es una <b>fila</b>, porque el sorteo elige filas al
 *       azar. El agrupamiento es de lectura; no se juntan participaciones en una fila.</li>
 *   <li><b>D2</b> — cada URL se valida segun su propio modo: {@code UNICA} la rechaza si ya
 *       existe en la rifa, {@code REPETIDA_PERMITIDA} la acepta igual.</li>
 * </ul>
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
@Service
public class GestionarBoletosService implements GestionarBoletosCasoUso {

    private static final Logger log = LoggerFactory.getLogger(GestionarBoletosService.class);

    private final BoletosDeRifaPort boletos;

    public GestionarBoletosService(BoletosDeRifaPort boletos) {
        this.boletos = boletos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrupoDeBoletos> verAgrupados(Integer rifaId) {
        return boletos.gruposDeLaRifa(rifaId);
    }

    /**
     * Todo o nada: si la tercera URL choca con una que ya existe, no queda ninguna cargada.
     * Media carga seria peor que ninguna — el admin no sabria cuales entraron.
     */
    @Override
    @Transactional
    public GrupoDeBoletos cargar(CargaDeParticipaciones peticion) {
        List<NuevaParticipacion> pedidas = conUrl(peticion.participaciones());
        if (pedidas.isEmpty()) {
            throw new CargaSinParticipacionesException();
        }

        PerfilEnRed perfil = new PerfilEnRed(peticion.plataforma(), peticion.urlPerfil());
        rechazarDuplicadasEntreSi(pedidas);

        for (NuevaParticipacion nueva : pedidas) {
            validarModo(peticion.rifaId(), nueva);
        }

        for (NuevaParticipacion nueva : pedidas) {
            boletos.crearParticipacion(peticion.concursanteId(), perfil,
                    Participacion.nueva(nueva.urlParticipacion(), nueva.motivo()));
        }

        log.info("Rifa {}: se cargaron {} participaciones de {} en {}", peticion.rifaId(),
                pedidas.size(), peticion.urlPerfil(), peticion.plataforma());
        return grupoOFalla(peticion.rifaId(), perfil);
    }

    @Override
    @Transactional
    public GrupoDeBoletos agregarParticipacion(Integer rifaId, Plataforma plataforma,
            String urlPerfil, NuevaParticipacion participacion) {
        PerfilEnRed perfil = new PerfilEnRed(plataforma, urlPerfil);
        GrupoDeBoletos grupo = grupoOFalla(rifaId, perfil);

        if (participacion == null || !tieneUrl(participacion)) {
            throw new CargaSinParticipacionesException();
        }
        validarModo(rifaId, participacion);

        boletos.crearParticipacion(grupo.concursanteId(), perfil,
                Participacion.nueva(participacion.urlParticipacion(), participacion.motivo()));

        log.info("Rifa {}: +1 participacion en el grupo {} de {}", rifaId, urlPerfil, plataforma);
        return grupoOFalla(rifaId, perfil);
    }

    /**
     * Quita una sola participacion. El grupo puede quedar vacio, y eso esta bien: significa
     * que el cliente sigue en la rifa por otras redes, o que se le quito todo lo que habia
     * cargado en esta.
     */
    @Override
    @Transactional
    public GrupoDeBoletos quitarParticipacion(Integer rifaId, Integer boletoId) {
        GrupoDeBoletos grupo = boletos.gruposDeLaRifa(rifaId).stream()
                .filter(g -> g.participaciones().stream().anyMatch(p -> boletoId.equals(p.boletoId())))
                .findFirst()
                .orElseThrow(() -> new GrupoNoEncontradoException(String.format(
                        "El boleto %d no existe en la rifa %d", boletoId, rifaId)));

        boletos.borrarParticipacion(boletoId);
        log.info("Rifa {}: se quito el boleto {} del grupo {}", rifaId, boletoId,
                grupo.perfil().urlPerfil());

        return boletos.buscarGrupo(rifaId, grupo.perfil())
                .orElseGet(() -> new GrupoDeBoletos(grupo.concursanteId(), grupo.nombreConcursante(),
                        grupo.perfil(), List.of()));
    }

    /**
     * Regla D2. Solo el modo {@code UNICA} mira la base; {@code REPETIDA_PERMITIDA} pasa
     * derecho a proposito.
     */
    private void validarModo(Integer rifaId, NuevaParticipacion nueva) {
        if (!ModoDeCarga.oPorDefecto(nueva.modo()).exigeQueNoExista()) {
            return;
        }
        Optional<BoletosDeRifaPort.DuenoDeLaUrl> dueno =
                boletos.buscarUrlEnLaRifa(rifaId, nueva.urlParticipacion());
        if (dueno.isPresent()) {
            throw new UrlParticipacionDuplicadaException(nueva.urlParticipacion(),
                    dueno.get().nombre(), dueno.get().boletoId());
        }
    }

    /**
     * Dos URLs iguales dentro del mismo alta. No llegan a la base, asi que
     * {@link #validarModo} no las veria: la primera todavia no esta guardada cuando se
     * valida la segunda.
     */
    private void rechazarDuplicadasEntreSi(List<NuevaParticipacion> pedidas) {
        List<String> vistas = new ArrayList<>();
        for (NuevaParticipacion nueva : pedidas) {
            String clave = PerfilEnRed.normalizar(nueva.urlParticipacion());
            if (vistas.contains(clave) && ModoDeCarga.oPorDefecto(nueva.modo()).exigeQueNoExista()) {
                throw new UrlParticipacionDuplicadaException(nueva.urlParticipacion(),
                        "este mismo alta", null);
            }
            vistas.add(clave);
        }
    }

    private GrupoDeBoletos grupoOFalla(Integer rifaId, PerfilEnRed perfil) {
        return boletos.buscarGrupo(rifaId, perfil)
                .orElseThrow(() -> new GrupoNoEncontradoException(String.format(
                        "No hay boletos cargados de '%s' en %s dentro de la rifa %d",
                        perfil.urlPerfil(), perfil.plataforma(), rifaId)));
    }

    private List<NuevaParticipacion> conUrl(List<NuevaParticipacion> participaciones) {
        if (participaciones == null) {
            return List.of();
        }
        return participaciones.stream().filter(this::tieneUrl).toList();
    }

    private boolean tieneUrl(NuevaParticipacion p) {
        return p != null && p.urlParticipacion() != null && !p.urlParticipacion().isBlank();
    }
}
