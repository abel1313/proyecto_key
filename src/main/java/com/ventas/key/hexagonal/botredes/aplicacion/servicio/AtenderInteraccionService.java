package com.ventas.key.hexagonal.botredes.aplicacion.servicio;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Accion;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Pausa;
import com.ventas.key.hexagonal.botredes.dominio.modelo.PoliticaDeRespuesta;
import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.modelo.ResultadoDelCerebro;
import com.ventas.key.hexagonal.botredes.dominio.puerto.entrada.AtenderInteraccionCasoUso;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.AvisoAdminPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CerebroPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ControlDeAbusoPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CuentaDelNegocioPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.PausasPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ProductoDePublicacionPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RegistroInteraccionesPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RespuestaEnRedPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atiende comentarios y mensajes directos de Facebook e Instagram con las reglas de
 * {@code botredes/README.md}.
 *
 * <p>Se procesa en el hilo del webhook (chatbot + Meta tardan pocos segundos). Si Meta empieza a
 * reintentar por timeouts, pasarlo a una cola.</p>
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
@Service
public class AtenderInteraccionService implements AtenderInteraccionCasoUso {

    private static final Logger log = LoggerFactory.getLogger(AtenderInteraccionService.class);

    private final RegistroInteraccionesPort registro;
    private final PausasPort pausas;
    private final CerebroPort cerebro;
    private final ProductoDePublicacionPort productos;
    private final RespuestaEnRedPort red;
    private final AvisoAdminPort avisoAdmin;
    private final ControlDeAbusoPort abuso;
    private final CuentaDelNegocioPort cuentaDelNegocio;
    private final Duration duracionPausa;
    private final Clock reloj;

    // A quién le está contestando el bot ahora mismo por mensaje directo. El eco de su propia
    // respuesta puede llegar antes de guardarla, y sin esto se confundía con el admin (R5).
    private final Set<String> respuestasEnCurso = ConcurrentHashMap.newKeySet();

    @Autowired
    public AtenderInteraccionService(RegistroInteraccionesPort registro, PausasPort pausas, CerebroPort cerebro,
                                     ProductoDePublicacionPort productos, RespuestaEnRedPort red,
                                     AvisoAdminPort avisoAdmin, ControlDeAbusoPort abuso,
                                     CuentaDelNegocioPort cuentaDelNegocio,
                                     @Value("${redes.bot.pausa-minutos:30}") long pausaMinutos) {
        this(registro, pausas, cerebro, productos, red, avisoAdmin, abuso, cuentaDelNegocio,
                Duration.ofMinutes(pausaMinutos), Clock.systemDefaultZone());
    }

    public AtenderInteraccionService(RegistroInteraccionesPort registro, PausasPort pausas, CerebroPort cerebro,
                                     ProductoDePublicacionPort productos, RespuestaEnRedPort red,
                                     AvisoAdminPort avisoAdmin, ControlDeAbusoPort abuso,
                                     CuentaDelNegocioPort cuentaDelNegocio, Duration duracionPausa, Clock reloj) {
        this.registro = registro;
        this.pausas = pausas;
        this.cerebro = cerebro;
        this.productos = productos;
        this.red = red;
        this.avisoAdmin = avisoAdmin;
        this.abuso = abuso;
        this.cuentaDelNegocio = cuentaDelNegocio;
        this.duracionPausa = duracionPausa;
        this.reloj = reloj;
    }

    @Override
    public void atender(Interaccion i) {
        if (cuentaDelNegocio.esDelNegocio(i.red(), i.autorId())) {
            if (i.canal() == Canal.COMENTARIO) {
                registrarRespuestaEnComentario(i);
            }
            return;
        }
        if (i.id() == null || (!i.tieneTexto() && !(i.esMensajeDirecto() && i.traeAdjunto()))) {
            log.info("{} {} de {} ignorado -- no trae nada que contestar", i.canal(), i.id(), i.red());
            return;
        }
        if (registro.yaProcesada(i.canal(), i.id())) {
            log.info("{} {} de {} ya fue procesado, se ignora el reenvío", i.canal(), i.id(), i.red());
            return;
        }
        if (enPausa(i)) {
            log.info("{} {} de {} ignorado -- una persona está atendiendo esa conversación", i.canal(), i.id(), i.red());
            return;
        }

        String clave = i.claveAbuso();
        if (abuso.bloqueado(clave)) {
            log.info("{} {} de {} ignorado -- autor {} en cooldown/bloqueado", i.canal(), i.id(), i.red(), clave);
            return;
        }
        if (abuso.limiteExcedido(clave)) {
            log.info("{} {} de {} ignorado -- autor {} alcanzó el límite por hora", i.canal(), i.id(), i.red(), clave);
            registro.guardar(i, null, null);
            return;
        }
        abuso.registrarMensaje(clave);

        boolean esPrimeraVez = i.autorId() == null || !registro.yaLeHabiamosEscrito(i.canal(), i.autorId());
        Accion accion = decidir(i, esPrimeraVez);
        ejecutar(i, accion, esPrimeraVez);
    }

    @Override
    public void registrarEcoDeMensaje(RedSocial redSocial, String mid, String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return;
        }
        if ((mid != null && registro.esRespuestaDelBot(Canal.MENSAJE_DIRECTO, mid))
                || respuestasEnCurso.contains(claveEnCurso(redSocial, clienteId))) {
            return;
        }
        pausas.pausar(Canal.MENSAJE_DIRECTO, clienteId, null, ahora());
        log.info("El admin contestó a mano por mensaje directo a {} en {} -- bot en pausa {} min",
                clienteId, redSocial, duracionPausa.toMinutes());
    }

    private void registrarRespuestaEnComentario(Interaccion i) {
        if (i.respondeA() == null || i.respondeA().isBlank()) {
            return;
        }
        if (registro.esRespuestaDelBot(Canal.COMENTARIO, i.id())) {
            return;
        }
        registro.comentarioOriginal(i.respondeA()).ifPresent(original -> {
            if (original.autorId() == null || original.publicacionId() == null) {
                return;
            }
            pausas.pausar(Canal.COMENTARIO, original.autorId(), original.publicacionId(), ahora());
            log.info("El admin contestó a mano el comentario {} en {} -- bot en pausa {} min para autor={} en post={}",
                    i.respondeA(), i.red(), duracionPausa.toMinutes(), original.autorId(), original.publicacionId());
        });
    }

    private boolean enPausa(Interaccion i) {
        if (i.autorId() == null) {
            return false;
        }
        String publicacion = i.canal() == Canal.COMENTARIO ? i.publicacionId() : null;
        if (i.canal() == Canal.COMENTARIO && publicacion == null) {
            return false;
        }
        return pausas.desde(i.canal(), i.autorId(), publicacion)
                .map(desde -> Pausa.vigente(desde, ahora(), duracionPausa))
                .orElse(false);
    }

    private Accion decidir(Interaccion i, boolean esPrimeraVez) {
        if (i.esMensajeDirecto() && !i.tieneTexto()) {
            return PoliticaDeRespuesta.paraAdjunto(i.canal(), esPrimeraVez);
        }
        String respuesta;
        try {
            respuesta = consultarCerebro(i, esPrimeraVez);
        } catch (RuntimeException e) {
            log.warn("El chatbot falló con {} {} de {}: {} -- se escala", i.canal(), i.id(), i.red(), e.getMessage());
            return PoliticaDeRespuesta.escalar(i.canal(), esPrimeraVez, "el chatbot no respondió");
        }
        ResultadoDelCerebro resultado = PoliticaDeRespuesta.interpretar(respuesta);
        if (resultado instanceof ResultadoDelCerebro.NoEntendido) {
            abuso.registrarNoEntendido(i.claveAbuso());
        } else {
            abuso.registrarEntendido(i.claveAbuso());
        }
        return PoliticaDeRespuesta.decidir(i.canal(), resultado, esPrimeraVez);
    }

    private String consultarCerebro(Interaccion i, boolean esPrimeraVez) {
        if (i.esMensajeDirecto()) {
            return cerebro.responderMensajeDirecto(i.red(), i.texto(), esPrimeraVez);
        }
        Optional<Integer> variante = i.publicacionId() == null ? Optional.empty() : productos.varianteDe(i.publicacionId());
        return variante.isPresent()
                ? cerebro.responderSobreProducto(i.red(), i.texto(), variante.get(), esPrimeraVez)
                : cerebro.responderComentarioSinProducto(i.red(), i.texto(), esPrimeraVez);
    }

    private void ejecutar(Interaccion i, Accion accion, boolean esPrimeraVez) {
        String enCurso = claveEnCurso(i.red(), i.autorId());
        if (i.esMensajeDirecto()) {
            respuestasEnCurso.add(enCurso);
        }
        try {
            String textoEnviado = accion.textoPublico();
            String idRespuesta = null;
            try {
                idRespuesta = i.esMensajeDirecto()
                        ? red.enviarMensaje(i.red(), i.autorId(), accion.textoPublico())
                        : red.responderComentario(i.red(), i.id(), accion.textoPublico());
                log.info("{} {} de {} respondido por el bot (primeraVez={}, escalado={})",
                        i.canal(), i.id(), i.red(), esPrimeraVez, accion.avisarAdmin());
            } catch (RuntimeException e) {
                log.warn("No se pudo responder {} {} en {}: {}", i.canal(), i.id(), i.red(), e.getMessage());
                textoEnviado = null;
            }
            if (accion.avisarAdmin()) {
                avisoAdmin.necesitaAtencion(i, accion.motivo());
                log.info("{} {} de {} escalado al admin ({})", i.canal(), i.id(), i.red(), accion.motivo());
            }
            boolean sePuedePausar = i.autorId() != null && (i.esMensajeDirecto() || i.publicacionId() != null);
            if (accion.pausar() && sePuedePausar) {
                pausas.pausar(i.canal(), i.autorId(), i.esMensajeDirecto() ? null : i.publicacionId(), ahora());
            }
            registro.guardar(i, textoEnviado, idRespuesta);
        } finally {
            respuestasEnCurso.remove(enCurso);
        }
    }

    private String claveEnCurso(RedSocial redSocial, String clienteId) {
        return redSocial.codigo() + ":" + clienteId;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
