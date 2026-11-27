package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DireccionMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Confirmacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.HorarioDeAtencion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TextosDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.InterpreteDeMarcas;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MarcaDePedidoInvalidaException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.SolicitudDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MotivoDeEscalada;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PoliticaDeRespuestaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaDelBot;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TurnoDeConversacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PedidoPorWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.RecibirMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.AvisoAlAdministradorPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ControlDeAbusoWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.HorarioDeAtencionPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Atiende lo que Meta manda por el webhook (reglas R1 a R11 de {@code whatsapp/README.md}):
 * guarda el mensaje una sola vez, abre la ventana de 24 h con su hora y, si ninguna persona atiende,
 * le contesta el bot.
 *
 * <p>Quien lo llama (el procesador del webhook) ya respondió 200 a Meta y serializa los mensajes de
 * un mismo número, así que aquí no hay que preocuparse por dos hilos con el mismo cliente.</p>
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
public class RecibirMensajeWhatsappService implements RecibirMensajeWhatsappCasoUso {

    private static final Logger log = LoggerFactory.getLogger(RecibirMensajeWhatsappService.class);

    /** Cuántos mensajes del hilo se le pasan al chatbot. */
    private static final int MAX_HISTORIAL = 12;

    static final String MOTIVO_PENDIENTE =
            "Respuesta pendiente: ya pasaron 24 h desde el último mensaje del cliente. Hace falta una plantilla aprobada o atención de una persona";

    private final ClientesWhatsappPort clientes;
    private final MensajesWhatsappPort mensajes;
    private final CerebroWhatsappPort cerebro;
    private final ControlDeAbusoWhatsappPort abuso;
    private final EnvioConVentanaService envio;
    private final Clock reloj;
    private final Duration pausaDelBot;
    private final HorarioDeAtencionPort horario;
    private final AvisoAlAdministradorPort aviso;
    /** null = el bot NO toma pedidos (interruptor {@code whatsapp.bot.pedidos-habilitado}, regla R16). */
    private final PedidoPorWhatsappCasoUso pedidos;

    public RecibirMensajeWhatsappService(ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes,
                                         CerebroWhatsappPort cerebro, ControlDeAbusoWhatsappPort abuso,
                                         EnvioConVentanaService envio, Clock reloj, Duration pausaDelBot,
                                         HorarioDeAtencionPort horario, AvisoAlAdministradorPort aviso,
                                         PedidoPorWhatsappCasoUso pedidos) {
        this.clientes = clientes;
        this.mensajes = mensajes;
        this.cerebro = cerebro;
        this.abuso = abuso;
        this.envio = envio;
        this.reloj = reloj;
        this.pausaDelBot = pausaDelBot;
        this.horario = horario;
        this.aviso = aviso;
        this.pedidos = pedidos;
    }

    @Override
    public void recibir(MensajeEntrante mensaje) {
        if (mensaje == null || vacio(mensaje.waMessageId()) || vacio(mensaje.telefono())) {
            log.info("WhatsApp: mensaje sin id o sin número, se ignora");
            return;
        }
        Instant ahora = reloj.instant();
        ClienteWhatsapp cliente = clientes.buscarOCrear(mensaje.telefono(), mensaje.nombrePerfil(), ahora);

        // R7: Meta reenvía. El id es único en la base, así que solo el primero sigue adelante.
        if (!mensajes.guardarEntrante(cliente.id(), mensaje)) {
            log.info("WhatsApp: mensaje {} ya fue procesado, se ignora el reenvío", mensaje.waMessageId());
            return;
        }

        // R1: la marca del último mensaje entrante es la que manda Meta, no la hora en que lo procesamos.
        Instant enviadoEn = mensaje.enviadoEn() != null ? mensaje.enviadoEn() : ahora;
        clientes.registrarEntrante(cliente.id(), enviadoEn, ahora, EnvioConVentanaService.resumen(textoParaLista(mensaje)));

        ClienteWhatsapp actual = clientes.porId(cliente.id()).orElse(cliente);
        if (actual.botPausado(ahora, pausaDelBot)) {
            // I5: con una persona atendiendo no puede quedar un resumen vivo que luego cree un segundo Apartado.
            descartarBorrador(actual.id());
            clientes.marcarAtencion(actual.id(), true);
            log.info("WhatsApp cliente {}: una persona atiende esta conversación, el bot no contesta", actual.id());
            return;
        }
        atenderConElBot(actual, mensaje);
    }

    @Override
    public void registrarEstadoDeEntrega(EstadoDeEntrega estado) {
        if (estado == null || vacio(estado.waMessageId())) {
            return;
        }
        mensajes.actualizarEstadoPorWaId(estado).ifPresent(clienteId -> {
            if (estado.estado() == EstadoMensaje.FALLIDO) {
                // R6: un fallo asíncrono (ej. 131047) se registra y se avisa a una persona. Jamás se reintenta.
                clientes.marcarAtencion(clienteId, true);
                log.warn("WhatsApp cliente {}: Meta avisó que el mensaje {} falló (código {}), sin reintento",
                        clienteId, estado.waMessageId(), estado.codigoError());
            }
        });
    }

    private void atenderConElBot(ClienteWhatsapp cliente, MensajeEntrante mensaje) {
        String clave = "whatsapp:" + cliente.telefono();
        if (abuso.bloqueado(clave) || abuso.limiteExcedido(clave)) {
            log.info("WhatsApp cliente {}: el bot no contesta (cooldown, bloqueo o límite por hora)", cliente.id());
            return;
        }
        abuso.registrarMensaje(clave);

        Instant ahora = reloj.instant();
        if (!cliente.ventanaAbierta(ahora)) {
            // R5: el mensaje ya cumplió 24 h (llegó tarde): nada de mensaje libre.
            dejarPendiente(cliente, ahora);
            return;
        }

        boolean esPrimeraVez = !mensajes.yaLeHabiamosEscrito(cliente.id());
        if (!mensaje.esTexto()) {
            escalar(cliente, MotivoDeEscalada.MANDO_UN_ADJUNTO, esPrimeraVez, textoParaLista(mensaje),
                    PoliticaDeRespuestaWhatsapp.saludoAdjunto(esPrimeraVez));
            return;
        }

        if (pedidos != null) {
            // R16: si hay un resumen esperando el «sí» del cliente, eso se resuelve aquí con reglas fijas, sin la IA.
            Optional<RespuestaDePedido> alConfirmar = pedidoAlResponder(cliente, mensaje);
            if (alConfirmar.isPresent()) {
                enviarRespuestaDePedido(cliente, alConfirmar.get());
                return;
            }
        }

        String cruda;
        try {
            String contextoDePedido = pedidos == null ? null : contextoDePedido(cliente);
            cruda = cerebro.responder(mensaje.texto(), historialPara(cliente.id(), mensaje), esPrimeraVez, contextoDePedido);
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: el chatbot falló ({}), se pasa a una persona", cliente.id(), e.getMessage());
            escalar(cliente, MotivoDeEscalada.FALLO_EL_ASISTENTE, esPrimeraVez, textoParaLista(mensaje),
                    PoliticaDeRespuestaWhatsapp.saludoEscalado(esPrimeraVez));
            return;
        }
        if (cruda != null && cruda.contains("##FAREWELL##")) {
            abuso.registrarNoEntendido(clave);
        } else {
            abuso.registrarEntendido(clave);
        }

        if (pedidos != null && atenderMarcasDePedido(cliente, cruda)) {
            return;
        }

        RespuestaDelBot respuesta = PoliticaDeRespuestaWhatsapp.interpretar(cruda, esPrimeraVez);
        if (respuesta instanceof RespuestaDelBot.Escalar escalar) {
            escalar(cliente, escalar.motivo(), esPrimeraVez, textoParaLista(mensaje), escalar.texto());
        } else if (respuesta instanceof RespuestaDelBot.Contestar contestar) {
            // R2 y R5: aquí se vuelve a validar la ventana con la hora de AHORA, por si el bot tardó.
            envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, contestar.texto());
        }
    }

    private Optional<RespuestaDePedido> pedidoAlResponder(ClienteWhatsapp cliente, MensajeEntrante mensaje) {
        try {
            Optional<RespuestaDePedido> r = pedidos.alResponder(cliente.id(), cliente.telefono(), cliente.nombre(),
                    mensaje.texto(), mensaje.enviadoEn());
            if (r.isPresent()) {
                return r;
            }
            // I3: un «sí» al resumen cuando ya no hay resumen (venció o el servidor se reinició) NO se deja al modelo: podría
            // decir que el pedido «quedó hecho». Se contesta con un texto fijo.
            if (Confirmacion.esAfirmativa(mensaje.texto()) && ultimoMensajeDelBotEsElResumen(cliente.id())) {
                return Optional.of(pedidos.resumenVencido());
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: falló el manejo del pedido ({}), sigue el bot normal", cliente.id(), e.toString());
            return Optional.empty();
        }
    }

    private boolean ultimoMensajeDelBotEsElResumen(long clienteId) {
        try {
            List<MensajeWhatsapp> recientes = mensajes.ultimos(clienteId, 6);
            for (int i = recientes.size() - 1; i >= 0; i--) {
                MensajeWhatsapp m = recientes.get(i);
                if (m.direccion() == DireccionMensaje.SALIENTE) {
                    return m.estado() != null && m.estado().salioAlCliente() && m.contenido() != null
                            && m.contenido().contains(TextosDePedido.PREGUNTA_CONFIRMAR);
                }
            }
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: no se pudo leer el hilo para revisar el resumen ({})", clienteId, e.toString());
        }
        return false;
    }

    private void descartarBorrador(long clienteId) {
        if (pedidos == null) {
            return;
        }
        try {
            pedidos.descartar(clienteId);
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: no se pudo descartar el resumen del pedido ({})", clienteId, e.toString());
        }
    }

    private String contextoDePedido(ClienteWhatsapp cliente) {
        try {
            return pedidos.contextoParaElModelo(cliente.telefono());
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: no se pudo armar el contexto del pedido ({})", cliente.id(), e.toString());
            return null;
        }
    }

    /**
     * R16. Si el modelo dejó una marca de pedido o de cancelación, el sistema la atiende y el texto del modelo NO se
     * manda (el resumen y los precios los arma el sistema). Devuelve true si ya contestó.
     */
    private boolean atenderMarcasDePedido(ClienteWhatsapp cliente, String cruda) {
        if (cruda != null && (cruda.contains("##HUMANO##") || cruda.contains("##ESCALAR##"))) {
            // M5: si el modelo pide una persona, gana la persona: el pedido que haya escrito junto se descarta.
            return false;
        }
        boolean pidePedido = InterpreteDeMarcas.contieneMarcaDePedido(cruda);
        boolean pideCancelar = InterpreteDeMarcas.contieneMarcaDeCancelacion(cruda);
        if (!pidePedido && !pideCancelar) {
            return false;
        }
        RespuestaDePedido respuesta;
        try {
            if (pidePedido) {
                SolicitudDePedido solicitud;
                try {
                    solicitud = InterpreteDeMarcas.leerPedido(cruda);
                } catch (MarcaDePedidoInvalidaException e) {
                    log.warn("WhatsApp cliente {}: la marca del pedido vino mal escrita ({})", cliente.id(), e.getMessage());
                    respuesta = pedidos.marcaIlegible();
                    enviarRespuestaDePedido(cliente, respuesta);
                    return true;
                }
                respuesta = pedidos.proponer(cliente.id(), cliente.telefono(), solicitud);
            } else {
                var numero = InterpreteDeMarcas.numeroDeCancelacion(cruda);
                respuesta = pedidos.solicitarCancelacion(cliente.telefono(), cliente.nombre(),
                        numero.isPresent() ? numero.getAsInt() : null);
            }
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: falló el pedido ({}), se pasa a una persona", cliente.id(), e.toString());
            descartarBorrador(cliente.id());
            escalar(cliente, MotivoDeEscalada.FALLO_EL_ASISTENTE, false, "[pedido]",
                    PoliticaDeRespuestaWhatsapp.saludoEscalado(false));
            return true;
        }
        enviarRespuestaDePedido(cliente, respuesta);
        return true;
    }

    /**
     * C1: el resumen solo vale (se guarda su borrador) si de verdad SALIÓ hacia el cliente (estado ENVIADO), y se guarda con la
     * hora de envío: un «sí» escrito antes no cuenta. Si Meta lo rechazó o la ventana se cerró, no queda ningún borrador vivo
     * y la conversación queda marcada para una persona.
     */
    private void enviarRespuestaDePedido(ClienteWhatsapp cliente, RespuestaDePedido respuesta) {
        if (respuesta.esIgnorar()) {
            return;
        }
        MensajeWhatsapp enviado = envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, respuesta.texto());
        boolean salio = enviado != null && enviado.estado() == EstadoMensaje.ENVIADO;
        if (respuesta.borradorPendiente() != null) {
            if (salio) {
                pedidos.resumenEnviado(cliente.id(), respuesta.borradorPendiente(),
                        enviado.enviadoEn() != null ? enviado.enviadoEn() : reloj.instant());
            } else {
                pedidos.descartar(cliente.id());
                clientes.marcarAtencion(cliente.id(), true);
                log.warn("WhatsApp cliente {}: el resumen del pedido no salió, no se guarda el borrador", cliente.id());
            }
        }
        if (respuesta.requiereAtencion()) {
            clientes.marcarAtencion(cliente.id(), true);
        }
    }

    /**
     * R12 y R13. Dentro del horario de atención: el bot saluda, avisa que lo atenderá una persona, se aparta (R10),
     * la conversación queda marcada y se le avisa al administrador por correo. FUERA del horario: no hay nadie, así
     * que no se promete que "en un momento" lo atienden: se le dice el horario y se le ofrece seguir con el bot; el bot
     * NO se aparta (sigue contestando) y la conversación queda marcada para verla al abrir, sin correo.
     * Sin horario configurado se trata como abierto y el bot no da ninguna hora.
     */
    private void escalar(ClienteWhatsapp cliente, MotivoDeEscalada motivo, boolean esPrimeraVez,
                         String ultimoMensaje, String textoEnHorario) {
        Optional<HorarioDeAtencion> h = consultarHorario();
        if (h.isPresent() && !h.get().abiertoEn(reloj.instant())) {
            envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, textoFueraDeHorario(cliente, motivo, esPrimeraVez, h.get()));
            clientes.marcarAtencion(cliente.id(), true);
            log.info("WhatsApp cliente {}: necesita una persona fuera del horario ({}), el bot sigue", cliente.id(), h.get().aTexto());
            return;
        }
        envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, textoEnHorario);
        descartarBorrador(cliente.id());
        clientes.pausarBot(cliente.id(), reloj.instant());
        clientes.marcarAtencion(cliente.id(), true);
        try {
            aviso.conversacionNecesitaPersona(cliente.telefono(), cliente.nombre(), ultimoMensaje, motivo);
        } catch (RuntimeException e) {
            // El correo es un extra: si falla, la conversación ya quedó marcada en la pantalla.
            log.warn("WhatsApp cliente {}: no se pudo avisar al administrador ({})", cliente.id(), e.toString());
        }
    }

    /** El texto según el motivo; si ya se le dijo el horario y insiste, no se repite el mismo aviso (R12). */
    private String textoFueraDeHorario(ClienteWhatsapp cliente, MotivoDeEscalada motivo, boolean esPrimeraVez,
                                       HorarioDeAtencion h) {
        return switch (motivo) {
            case MANDO_UN_ADJUNTO -> PoliticaDeRespuestaWhatsapp.adjuntoFueraDeHorario(esPrimeraVez, h);
            case FALLO_EL_ASISTENTE -> PoliticaDeRespuestaWhatsapp.fallaFueraDeHorario(esPrimeraVez, h);
            case PIDIO_UNA_PERSONA -> yaSeLeDijoElHorario(cliente.id())
                    ? PoliticaDeRespuestaWhatsapp.fueraDeHorarioRepetido(h)
                    : PoliticaDeRespuestaWhatsapp.fueraDeHorario(esPrimeraVez, h);
        };
    }

    /** ¿Lo último que le mandamos al cliente fue el aviso de que no hay nadie? */
    private boolean yaSeLeDijoElHorario(long clienteId) {
        try {
            List<MensajeWhatsapp> recientes = mensajes.ultimos(clienteId, 6);
            for (int i = recientes.size() - 1; i >= 0; i--) {
                MensajeWhatsapp m = recientes.get(i);
                if (m.direccion() == DireccionMensaje.SALIENTE) {
                    // El aviso largo o el corto: así el tercer intento también recibe el corto (hallazgo M6).
                    return m.contenido() != null
                            && (m.contenido().contains(PoliticaDeRespuestaWhatsapp.INICIO_AVISO_FUERA_DE_HORARIO)
                            || m.contenido().contains(PoliticaDeRespuestaWhatsapp.INICIO_AVISO_REPETIDO));
                }
            }
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: no se pudo leer el hilo para no repetir el aviso ({})", clienteId, e.toString());
        }
        return false;
    }

    private Optional<HorarioDeAtencion> consultarHorario() {
        try {
            return horario.actual();
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo leer el horario del negocio, se trata como abierto ({})", e.toString());
            return Optional.empty();
        }
    }

    private void dejarPendiente(ClienteWhatsapp cliente, Instant ahora) {
        mensajes.guardarSaliente(cliente.id(), OrigenMensaje.SISTEMA, "text", MOTIVO_PENDIENTE,
                EstadoMensaje.PENDIENTE_PLANTILLA, null, MOTIVO_PENDIENTE, null, null, ahora);
        clientes.marcarAtencion(cliente.id(), true);
        log.info("WhatsApp cliente {}: ventana cerrada al llegar el mensaje, respuesta pendiente", cliente.id());
    }

    /** El hilo reciente, sin el mensaje que se está contestando y sin lo que nunca salió al cliente. */
    private List<TurnoDeConversacion> historialPara(long clienteId, MensajeEntrante actual) {
        List<MensajeWhatsapp> recientes = new ArrayList<>(mensajes.ultimos(clienteId, MAX_HISTORIAL + 1));
        if (!recientes.isEmpty() && actual.waMessageId().equals(recientes.get(recientes.size() - 1).waMessageId())) {
            recientes.remove(recientes.size() - 1);
        }
        List<TurnoDeConversacion> turnos = new ArrayList<>();
        for (MensajeWhatsapp m : recientes) {
            if (m.contenido() == null || m.contenido().isBlank() || !"text".equals(m.tipo())) {
                continue;
            }
            if (m.direccion() == DireccionMensaje.ENTRANTE) {
                turnos.add(new TurnoDeConversacion(TurnoDeConversacion.Rol.CLIENTE, m.contenido()));
            } else if (m.estado().salioAlCliente()) {
                turnos.add(new TurnoDeConversacion(m.origen() == OrigenMensaje.PERSONA
                        ? TurnoDeConversacion.Rol.PERSONA : TurnoDeConversacion.Rol.BOT, m.contenido()));
            }
        }
        return turnos;
    }

    private static String textoParaLista(MensajeEntrante m) {
        if (m.texto() != null && !m.texto().isBlank()) {
            return m.texto();
        }
        return "[" + (m.tipo() == null ? "mensaje" : m.tipo()) + "]";
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
