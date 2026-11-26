package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DireccionMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PoliticaDeRespuestaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaDelBot;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TurnoDeConversacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.RecibirMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ControlDeAbusoWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public RecibirMensajeWhatsappService(ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes,
                                         CerebroWhatsappPort cerebro, ControlDeAbusoWhatsappPort abuso,
                                         EnvioConVentanaService envio, Clock reloj, Duration pausaDelBot) {
        this.clientes = clientes;
        this.mensajes = mensajes;
        this.cerebro = cerebro;
        this.abuso = abuso;
        this.envio = envio;
        this.reloj = reloj;
        this.pausaDelBot = pausaDelBot;
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
            escalar(cliente, PoliticaDeRespuestaWhatsapp.saludoAdjunto(esPrimeraVez));
            return;
        }

        String cruda;
        try {
            cruda = cerebro.responder(mensaje.texto(), historialPara(cliente.id(), mensaje), esPrimeraVez);
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: el chatbot falló ({}), se pasa a una persona", cliente.id(), e.getMessage());
            escalar(cliente, PoliticaDeRespuestaWhatsapp.saludoEscalado(esPrimeraVez));
            return;
        }
        if (cruda != null && cruda.contains("##FAREWELL##")) {
            abuso.registrarNoEntendido(clave);
        } else {
            abuso.registrarEntendido(clave);
        }

        RespuestaDelBot respuesta = PoliticaDeRespuestaWhatsapp.interpretar(cruda, esPrimeraVez);
        if (respuesta instanceof RespuestaDelBot.Escalar escalar) {
            escalar(cliente, escalar.texto());
        } else if (respuesta instanceof RespuestaDelBot.Contestar contestar) {
            // R2 y R5: aquí se vuelve a validar la ventana con la hora de AHORA, por si el bot tardó.
            envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, contestar.texto());
        }
    }

    /** El bot saluda, avisa que lo atenderá una persona, se aparta (regla R10) y la conversación queda marcada. */
    private void escalar(ClienteWhatsapp cliente, String texto) {
        envio.enviarTexto(cliente.id(), OrigenMensaje.BOT, texto);
        clientes.pausarBot(cliente.id(), reloj.instant());
        clientes.marcarAtencion(cliente.id(), true);
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
