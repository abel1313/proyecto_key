package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.ConversacionNoEncontradaException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DecisionDeEnvio;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PoliticaDeEnvio;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoEnvio;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.EnvioWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

/**
 * El ÚNICO lugar por donde sale un mensaje hacia un cliente (bot y personas por igual), para que la
 * regla de la ventana de 24 h no se pueda saltar (reglas R1 a R6 de {@code whatsapp/README.md}).
 *
 * <p>Antes de cada envío vuelve a leer al cliente: entre que el bot empezó a pensar y la respuesta
 * quedó lista pudo pasar el tiempo, así que la ventana se valida con la hora de AHORA y con la
 * marca más reciente de su último mensaje. Todo queda en el historial, enviado o no, con su motivo.
 * Nunca se reintenta solo.</p>
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
public class EnvioConVentanaService {

    private static final Logger log = LoggerFactory.getLogger(EnvioConVentanaService.class);

    /** Límite de un mensaje de texto en WhatsApp. */
    static final int LIMITE_TEXTO = 4096;
    private static final int LIMITE_MOTIVO = 255;

    private final ClientesWhatsappPort clientes;
    private final MensajesWhatsappPort mensajes;
    private final EnvioWhatsappPort envio;
    private final Clock reloj;
    private final boolean metodoDePagoActivo;

    public EnvioConVentanaService(ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes,
                                  EnvioWhatsappPort envio, Clock reloj, boolean metodoDePagoActivo) {
        this.clientes = clientes;
        this.mensajes = mensajes;
        this.envio = envio;
        this.reloj = reloj;
        this.metodoDePagoActivo = metodoDePagoActivo;
    }

    /** Mensaje libre de texto: solo con la ventana abierta. */
    public MensajeWhatsapp enviarTexto(long clienteId, OrigenMensaje origen, String texto) {
        String cuerpo = recortar(texto);
        ClienteWhatsapp cliente = cliente(clienteId);
        Instant ahora = reloj.instant();
        DecisionDeEnvio decision = PoliticaDeEnvio.paraMensajeLibre(cliente.ultimoEntranteAt(), ahora);
        if (decision instanceof DecisionDeEnvio.NoEnviar noEnviar) {
            return noSalio(cliente, origen, "text", cuerpo, null, noEnviar, ahora);
        }
        return registrar(cliente, origen, "text", cuerpo, null, envio.enviarTexto(cliente.telefono(), cuerpo), ahora);
    }

    /** Imagen por enlace público con su pie: mensaje libre, solo con la ventana abierta. */
    public MensajeWhatsapp enviarImagen(long clienteId, OrigenMensaje origen, String urlImagen, String pie) {
        ClienteWhatsapp cliente = cliente(clienteId);
        Instant ahora = reloj.instant();
        String contenido = urlImagen + (pie == null || pie.isBlank() ? "" : "\n" + recortar(pie));
        DecisionDeEnvio decision = PoliticaDeEnvio.paraMensajeLibre(cliente.ultimoEntranteAt(), ahora);
        if (decision instanceof DecisionDeEnvio.NoEnviar noEnviar) {
            return noSalio(cliente, origen, "image", contenido, null, noEnviar, ahora);
        }
        return registrar(cliente, origen, "image", contenido, null,
                envio.enviarImagen(cliente.telefono(), urlImagen, pie), ahora);
    }

    /** Plantilla aprobada: dentro o fuera de la ventana, pero solo con método de pago válido. */
    public MensajeWhatsapp enviarPlantilla(long clienteId, OrigenMensaje origen, PlantillaWhatsapp plantilla) {
        ClienteWhatsapp cliente = cliente(clienteId);
        Instant ahora = reloj.instant();
        String contenido = plantilla.nombre() + (plantilla.parametros().isEmpty() ? "" : " " + plantilla.parametros());
        DecisionDeEnvio decision = PoliticaDeEnvio.paraPlantilla(metodoDePagoActivo);
        if (decision instanceof DecisionDeEnvio.NoEnviar noEnviar) {
            return noSalio(cliente, origen, "template", contenido, plantilla.nombre(), noEnviar, ahora);
        }
        return registrar(cliente, origen, "template", contenido, plantilla.nombre(),
                envio.enviarPlantilla(cliente.telefono(), plantilla), ahora);
    }

    private MensajeWhatsapp noSalio(ClienteWhatsapp cliente, OrigenMensaje origen, String tipo, String contenido,
                                    String plantilla, DecisionDeEnvio.NoEnviar decision, Instant ahora) {
        // Una respuesta del bot que ya no se puede mandar queda pendiente de plantilla o de una persona.
        EstadoMensaje estado = origen == OrigenMensaje.BOT && decision.estado() == EstadoMensaje.BLOQUEADO_VENTANA
                ? EstadoMensaje.PENDIENTE_PLANTILLA : decision.estado();
        MensajeWhatsapp guardado = mensajes.guardarSaliente(cliente.id(), origen, tipo, contenido, estado, null,
                decision.motivo(), null, plantilla, ahora);
        clientes.marcarAtencion(cliente.id(), true);
        log.info("WhatsApp cliente {}: mensaje {} no enviado ({}): {}", cliente.id(), tipo, estado, decision.motivo());
        return guardado;
    }

    private MensajeWhatsapp registrar(ClienteWhatsapp cliente, OrigenMensaje origen, String tipo, String contenido,
                                      String plantilla, ResultadoEnvio resultado, Instant ahora) {
        if (resultado instanceof ResultadoEnvio.Enviado enviado) {
            MensajeWhatsapp guardado = mensajes.guardarSaliente(cliente.id(), origen, tipo, contenido,
                    EstadoMensaje.ENVIADO, enviado.waMessageId(), null, null, plantilla, ahora);
            clientes.registrarSaliente(cliente.id(), resumen(contenido), ahora);
            return guardado;
        }
        ResultadoEnvio.Rechazado rechazado = (ResultadoEnvio.Rechazado) resultado;
        // 131047: Meta rechaza el mensaje libre por estar fuera de la ventana. Se registra y NO se reintenta.
        String motivo = rechazado.esFueraDeVentana()
                ? "Re-engagement message (131047): Meta rechazó el mensaje por estar fuera de la ventana de 24 h. No se reintenta"
                : rechazado.motivo();
        MensajeWhatsapp guardado = mensajes.guardarSaliente(cliente.id(), origen, tipo, contenido,
                EstadoMensaje.FALLIDO, null, recortarMotivo(motivo), rechazado.codigo(), plantilla, ahora);
        clientes.marcarAtencion(cliente.id(), true);
        log.warn("WhatsApp cliente {}: Meta rechazó el mensaje {} (código {}), sin reintento", cliente.id(), tipo,
                rechazado.codigo());
        return guardado;
    }

    private ClienteWhatsapp cliente(long id) {
        return clientes.porId(id).orElseThrow(() -> new ConversacionNoEncontradaException(id));
    }

    private static String recortar(String texto) {
        String t = texto == null ? "" : texto.trim();
        return t.length() > LIMITE_TEXTO ? t.substring(0, LIMITE_TEXTO - 1) + "…" : t;
    }

    private static String recortarMotivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        return motivo.length() > LIMITE_MOTIVO ? motivo.substring(0, LIMITE_MOTIVO - 1) + "…" : motivo;
    }

    static String resumen(String texto) {
        if (texto == null) {
            return null;
        }
        String una = texto.replaceAll("\\s+", " ").trim();
        return una.length() > 200 ? una.substring(0, 199) + "…" : una;
    }
}
