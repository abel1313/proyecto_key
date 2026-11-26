package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.ConversacionNoEncontradaException;
import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.EnvioRechazadoException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResumenDeConversacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.VentanaDe24Horas;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.AdministrarConversacionesWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Lo que hace una persona desde la pantalla (reglas R4 y R10 de {@code whatsapp/README.md}).
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
public class AdministrarConversacionesService implements AdministrarConversacionesWhatsappCasoUso {

    private static final int MAX_SIZE = 100;

    private final ClientesWhatsappPort clientes;
    private final MensajesWhatsappPort mensajes;
    private final EnvioConVentanaService envio;
    private final Clock reloj;
    private final Duration pausaDelBot;

    public AdministrarConversacionesService(ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes,
                                            EnvioConVentanaService envio, Clock reloj, Duration pausaDelBot) {
        this.clientes = clientes;
        this.mensajes = mensajes;
        this.envio = envio;
        this.reloj = reloj;
        this.pausaDelBot = pausaDelBot;
    }

    @Override
    public Pagina<ResumenDeConversacion> listar(int pagina, int size) {
        Instant ahora = reloj.instant();
        return clientes.listar(Math.max(pagina, 0), limitar(size)).mapear(c -> resumen(c, ahora));
    }

    @Override
    public ResumenDeConversacion conversacion(long clienteId) {
        return resumen(cliente(clienteId), reloj.instant());
    }

    @Override
    public Pagina<MensajeWhatsapp> mensajes(long clienteId, int pagina, int size) {
        cliente(clienteId);
        return mensajes.historial(clienteId, Math.max(pagina, 0), limitar(size));
    }

    @Override
    public MensajeWhatsapp responder(long clienteId, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escribe un mensaje");
        }
        MensajeWhatsapp registro = envio.enviarTexto(clienteId, OrigenMensaje.PERSONA, texto);
        return alTerminar(clienteId, registro);
    }

    @Override
    public MensajeWhatsapp enviarPlantilla(long clienteId, PlantillaWhatsapp plantilla) {
        MensajeWhatsapp registro = envio.enviarPlantilla(clienteId, OrigenMensaje.PERSONA, plantilla);
        return alTerminar(clienteId, registro);
    }

    @Override
    public ResumenDeConversacion tomar(long clienteId) {
        cliente(clienteId);
        clientes.tomarConversacion(clienteId, reloj.instant());
        return conversacion(clienteId);
    }

    @Override
    public ResumenDeConversacion devolverAlBot(long clienteId) {
        cliente(clienteId);
        clientes.reanudarBot(clienteId);
        clientes.marcarAtencion(clienteId, false);
        return conversacion(clienteId);
    }

    @Override
    public ResumenDeConversacion marcarAtendida(long clienteId) {
        cliente(clienteId);
        clientes.marcarAtencion(clienteId, false);
        return conversacion(clienteId);
    }

    /** Si salió, el bot se aparta (R10) y ya no hace falta la marca; si no salió, el historial ya trae el motivo. */
    private MensajeWhatsapp alTerminar(long clienteId, MensajeWhatsapp registro) {
        if (registro.estado() == EstadoMensaje.ENVIADO) {
            clientes.pausarBot(clienteId, reloj.instant());
            clientes.marcarAtencion(clienteId, false);
            return registro;
        }
        throw new EnvioRechazadoException(registro);
    }

    private ResumenDeConversacion resumen(ClienteWhatsapp c, Instant ahora) {
        boolean abierta = c.ventanaAbierta(ahora);
        return new ResumenDeConversacion(c, c.botPausado(ahora, pausaDelBot), abierta,
                abierta ? VentanaDe24Horas.cierraEn(c.ultimoEntranteAt()) : null,
                mensajes.contarSinResponder(c.id()));
    }

    private ClienteWhatsapp cliente(long id) {
        return clientes.porId(id).orElseThrow(() -> new ConversacionNoEncontradaException(id));
    }

    private static int limitar(int size) {
        return size <= 0 ? 30 : Math.min(size, MAX_SIZE);
    }
}
