package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.botredes.dominio.modelo.PoliticaDeRespuesta;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ControlDeAbusoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.AccionBot;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoPublicacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ModoVenta;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ProductoParaPublicar;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ReferenciaDePublicacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaBot;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestasSinIa;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.ResponderMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.AvisoAdminWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ContactoNegocioPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ProductoParaPublicarPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PublicacionesWhatsappPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Hexagonal: aplicacion] [Clean: Use Case Interactor]
 *
 * <p>Decide que hace el bot con un mensaje de WhatsApp (PLAN_WHATSAPP_PUBLICACIONES.md, secciones 5 a 7):
 * <ol>
 *   <li>En un GRUPO solo se contesta lo de una publicacion del bot: se sabe por el codigo (J42) en el texto o en la cita. Sin codigo,
 *       solo si mencionan al bot y hay UNA publicacion activa en ese grupo en las ultimas 24 h (si hay varias, pregunta de cual; sin
 *       ninguna, se ignora). Un codigo que no es de una publicacion PUBLICADA en ese grupo se ignora.</li>
 *   <li>Primero plantillas sin IA (precio, existencias, saludo, gracias); la IA solo si hace falta, con SOLO el producto.</li>
 *   <li>Una pregunta que no se cubre se escala: avisa al admin y el bot se aparta {@code redes.bot.pausa-minutos} de esa persona y publicacion.</li>
 *   <li>Quiere comprar: se avisa al admin y se le indica como seguir (por privado con enlace wa.me, o en el grupo para apartar).
 *       El pedido en si ({@code /venta}) todavia no se genera aqui.</li>
 * </ol>
 * La pausa vive en memoria: con un solo pod alcanza (igual que el eco de mensajes directos en botredes).
 */
@Service
public class ResponderMensajeWhatsappService implements ResponderMensajeWhatsappCasoUso {

    private static final Logger log = LoggerFactory.getLogger(ResponderMensajeWhatsappService.class);

    static final Duration VENTANA_SIN_CODIGO = Duration.ofHours(24);
    static final String ESPERA = "En un momento te atendemos 💖";

    private final PublicacionesWhatsappPort publicaciones;
    private final ProductoParaPublicarPort productos;
    private final CerebroWhatsappPort cerebro;
    private final AvisoAdminWhatsappPort avisoAdmin;
    private final ContactoNegocioPort contacto;
    private final ControlDeAbusoPort abuso;
    private final Duration duracionPausa;
    private final Clock reloj;
    private final Map<String, Instant> pausas = new ConcurrentHashMap<>();

    @Autowired
    public ResponderMensajeWhatsappService(PublicacionesWhatsappPort publicaciones, ProductoParaPublicarPort productos,
                                           CerebroWhatsappPort cerebro, AvisoAdminWhatsappPort avisoAdmin,
                                           ContactoNegocioPort contacto, ControlDeAbusoPort abuso,
                                           @Value("${redes.bot.pausa-minutos:30}") long pausaMinutos) {
        this(publicaciones, productos, cerebro, avisoAdmin, contacto, abuso, Duration.ofMinutes(pausaMinutos), Clock.systemDefaultZone());
    }

    public ResponderMensajeWhatsappService(PublicacionesWhatsappPort publicaciones, ProductoParaPublicarPort productos,
                                           CerebroWhatsappPort cerebro, AvisoAdminWhatsappPort avisoAdmin,
                                           ContactoNegocioPort contacto, ControlDeAbusoPort abuso, Duration duracionPausa, Clock reloj) {
        this.publicaciones = publicaciones;
        this.productos = productos;
        this.cerebro = cerebro;
        this.avisoAdmin = avisoAdmin;
        this.contacto = contacto;
        this.abuso = abuso;
        this.duracionPausa = duracionPausa;
        this.reloj = reloj;
    }

    /** Lo que se decidio de que publicacion hablan: una, ninguna (ignorar) o una respuesta directa (preguntar de cual). */
    private record Resolucion(PublicacionWhatsapp publicacion, boolean ladeado, RespuestaBot directa) {
        static Resolucion de(PublicacionWhatsapp p, boolean sinCodigo) {
            return new Resolucion(p, sinCodigo, null);
        }

        static Resolucion ninguna() {
            return new Resolucion(null, false, RespuestaBot.ignorar());
        }
    }

    @Override
    public RespuestaBot atender(MensajeWhatsapp m) {
        if (m == null || m.canal() == null || m.chat() == null || m.chat().isBlank() || m.texto() == null || m.texto().isBlank()) {
            return RespuestaBot.ignorar();
        }
        String ref = ReferenciaDePublicacion.normalizar(m.ref())
                .or(() -> ReferenciaDePublicacion.en(m.texto(), m.citaTexto())).orElse(null);

        Resolucion r = resolver(m, ref);
        if (r.directa() != null && r.directa().accion() == AccionBot.IGNORAR) {
            return r.directa();
        }
        PublicacionWhatsapp pub = r.publicacion();

        String clave = "whatsapp:" + m.chat().trim() + ":" + (m.remitente() == null ? "" : m.remitente().trim());
        if (enPausa(clave + ":" + (pub == null ? "-" : pub.codigoRef()))) {
            return RespuestaBot.ignorar();
        }
        if (abuso.bloqueado(clave) || abuso.limiteExcedido(clave)) {
            log.info("WhatsApp: {} ignorado por control de abuso", clave);
            return RespuestaBot.ignorar();
        }
        abuso.registrarMensaje(clave);

        if (r.directa() != null) {
            return r.directa();
        }
        return pub == null ? privadoSinPublicacion(m) : sobrePublicacion(m, pub, r.ladeado());
    }

    // ---- de que publicacion hablan -----------------------------------------------------------

    private Resolucion resolver(MensajeWhatsapp m, String ref) {
        if (ref != null) {
            Optional<PublicacionWhatsapp> pub = publicaciones.buscarPorCodigo(ref).filter(p -> p.estado() == EstadoPublicacion.PUBLICADA);
            if (m.esGrupo()) {
                // En un grupo, un codigo que no es de una publicacion PUBLICADA en ESE grupo no cuenta.
                return pub.filter(p -> mismoGrupo(p, m.chat())).map(p -> Resolucion.de(p, false)).orElse(Resolucion.ninguna());
            }
            return pub.map(p -> Resolucion.de(p, false)).orElse(Resolucion.de(null, false));
        }
        if (!m.esGrupo()) {
            return Resolucion.de(null, false);
        }
        if (!m.mencion()) {
            return Resolucion.ninguna(); // sin codigo, sin cita y sin mencion: charla del grupo
        }
        List<PublicacionWhatsapp> activas = publicaciones.publicadasDelGrupoDesde(m.chat().trim(),
                LocalDateTime.now(reloj).minus(VENTANA_SIN_CODIGO));
        if (activas.isEmpty()) {
            return Resolucion.ninguna();
        }
        if (activas.size() == 1) {
            return Resolucion.de(activas.get(0), true);
        }
        String codigos = String.join(", ", activas.stream().map(PublicacionWhatsapp::codigoRef).toList());
        return new Resolucion(null, false, new RespuestaBot(AccionBot.RESPONDER,
                "Tengo varias publicaciones activas (" + codigos + "). ¿De cuál me hablas? Escríbeme su código 💖", null, null));
    }

    private static boolean mismoGrupo(PublicacionWhatsapp p, String chat) {
        return p.grupo() != null && p.grupo().trim().equalsIgnoreCase(chat.trim());
    }

    // ---- contestar sobre una publicacion -------------------------------------------------------

    private RespuestaBot sobrePublicacion(MensajeWhatsapp m, PublicacionWhatsapp pub, boolean ladeado) {
        Optional<ProductoParaPublicar> encontrado = productos.de(pub.varianteId());
        if (encontrado.isEmpty()) {
            return responder(pub, "Ese artículo ya no está disponible 💖");
        }
        ProductoParaPublicar producto = encontrado.get();
        String prefijo = ladeado ? "Sobre " + nombreCorto(producto) + " (" + pub.codigoRef() + "): " : "";

        if (RespuestasSinIa.esCompra(m.texto())) {
            return compra(m, pub);
        }
        Optional<String> plantilla = RespuestasSinIa.para(m.texto(), producto);
        if (plantilla.isPresent()) {
            return responder(pub, prefijo + plantilla.get());
        }
        String crudo;
        try {
            crudo = cerebro.responderSobreProducto(m.texto(), pub.varianteId(), m.esGrupo());
        } catch (RuntimeException e) {
            log.warn("WhatsApp: el chatbot no pudo contestar sobre {}: {}", pub.codigoRef(), e.getMessage());
            return escalar(m, pub, "el chatbot no pudo contestar (falló o tardó)");
        }
        return interpretar(m, pub, crudo, prefijo);
    }

    private RespuestaBot privadoSinPublicacion(MensajeWhatsapp m) {
        String crudo;
        try {
            crudo = cerebro.responderMensajeDirecto(m.texto(), false);
        } catch (RuntimeException e) {
            log.warn("WhatsApp: el chatbot no pudo contestar un mensaje privado: {}", e.getMessage());
            return escalar(m, null, "el chatbot no pudo contestar (falló o tardó)");
        }
        return interpretar(m, null, crudo, "");
    }

    /** Lee las marcas del chatbot: ##ESCALAR## y ##GRACIAS##; el resto es texto para el cliente. */
    private RespuestaBot interpretar(MensajeWhatsapp m, PublicacionWhatsapp pub, String crudo, String prefijo) {
        String texto = crudo == null ? "" : crudo.trim();
        if (texto.contains("##ESCALAR##")) {
            return escalar(m, pub, "una pregunta que el producto o el catálogo no cubre");
        }
        if (texto.contains("##GRACIAS##")) {
            String frase = texto.replace("##GRACIAS##", "").trim();
            return responder(pub, frase.isBlank() || frase.length() > 120 ? "¡Muchas gracias! 💖" : frase);
        }
        texto = texto.replaceAll("##[A-Z]+(\\[[^\\]]*\\])?##", "").trim();
        if (texto.isBlank() || PoliticaDeRespuesta.queMenciona(texto, false).isPresent()) {
            return escalar(m, pub, "la respuesta de la IA no era segura para mandarla");
        }
        return responder(pub, prefijo + texto);
    }

    // ---- compra -------------------------------------------------------------------------------

    private RespuestaBot compra(MensajeWhatsapp m, PublicacionWhatsapp pub) {
        avisar(m, pub, "quiere comprar o apartar");
        pausar(m, pub);
        String nombre = primerNombre(m.remitente());
        String saludo = nombre.isEmpty() ? "¡Claro!" : "¡Claro, " + nombre + "!";
        if (!m.esGrupo()) {
            return new RespuestaBot(AccionBot.ESCALADO, "¡Perfecto! 💖 ¿Cuántas piezas quieres y lo recoges o te lo enviamos?",
                    pub.id(), pub.modoVenta());
        }
        if (pub.modoVenta() == ModoVenta.GRUPO) {
            return new RespuestaBot(AccionBot.ESCALADO,
                    saludo + " Te lo aparto 💖 ¿Cuántas piezas quieres? Los datos de entrega te los pido por privado.",
                    pub.id(), pub.modoVenta());
        }
        String enlace = contacto.whatsappUrl().map(u -> u + (u.contains("?") ? "&" : "?") + "text="
                + URLEncoder.encode("Quiero " + pub.codigoRef(), StandardCharsets.UTF_8)).orElse(null);
        return new RespuestaBot(AccionBot.OFRECER_PRIVADO,
                saludo + " Escríbeme por privado y te lo aparto 💖" + (enlace == null ? "" : "\n" + enlace), pub.id(), pub.modoVenta());
    }

    // ---- escalar y pausar -----------------------------------------------------------------------

    private RespuestaBot escalar(MensajeWhatsapp m, PublicacionWhatsapp pub, String motivo) {
        avisar(m, pub, motivo);
        pausar(m, pub);
        return new RespuestaBot(AccionBot.ESCALADO, ESPERA, pub == null ? null : pub.id(), pub == null ? null : pub.modoVenta());
    }

    private void avisar(MensajeWhatsapp m, PublicacionWhatsapp pub, String motivo) {
        try {
            avisoAdmin.necesitaAtencion(m, pub == null ? null : pub.codigoRef(), motivo);
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo avisar al admin ({}): {}", motivo, e.getMessage());
        }
    }

    private void pausar(MensajeWhatsapp m, PublicacionWhatsapp pub) {
        pausas.put(clavePausa(m, pub), Instant.now(reloj));
    }

    private boolean enPausa(String clave) {
        Instant desde = pausas.get(clave);
        if (desde == null) {
            return false;
        }
        if (Instant.now(reloj).isBefore(desde.plus(duracionPausa))) {
            return true;
        }
        pausas.remove(clave);
        return false;
    }

    private static String clavePausa(MensajeWhatsapp m, PublicacionWhatsapp pub) {
        return "whatsapp:" + m.chat().trim() + ":" + (m.remitente() == null ? "" : m.remitente().trim()) + ":"
                + (pub == null ? "-" : pub.codigoRef());
    }

    // ---- utilidades -----------------------------------------------------------------------------

    private static RespuestaBot responder(PublicacionWhatsapp pub, String texto) {
        return new RespuestaBot(AccionBot.RESPONDER, texto, pub == null ? null : pub.id(), pub == null ? null : pub.modoVenta());
    }

    private static String nombreCorto(ProductoParaPublicar p) {
        return p.nombre() == null || p.nombre().isBlank() ? "el artículo" : p.nombre().trim();
    }

    private static String primerNombre(String remitente) {
        if (remitente == null || remitente.isBlank()) {
            return "";
        }
        String limpio = remitente.trim().replaceAll("^[~@+]+", "");
        // un numero de telefono no es un nombre
        return limpio.matches("[\\d\\s+()-]+") ? "" : limpio.split("\\s+")[0];
    }
}
