package com.ventas.key.mis.productos.chatbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatbotBlockService {

    private static final int MAX_NO_COMPRENSIONES = 3;
    private static final Duration BLOQUEO_DURACION = Duration.ofHours(30);
    private static final Duration COOLDOWN_FAREWELL = Duration.ofSeconds(45);

    // Limite de mensajes por hora, independiente de si el bot entiende o no la pregunta -- evita
    // que alguien agote la cuota de OpenAI haciendo preguntas validas sin parar. Ventana movil:
    // arranca a contar desde el primer mensaje y se reinicia sola 1 hora despues de ese primer
    // mensaje (no es un contador fijo por reloj de pared).
    private static final int MAX_MENSAJES_POR_HORA = 20;
    private static final Duration VENTANA_LIMITE = Duration.ofHours(1);

    private final Map<String, IpInfo> ipInfoMap = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String ip) {
        IpInfo info = ipInfoMap.get(ip);
        if (info == null || info.bloqueadoHasta == null) return false;
        if (Instant.now().isAfter(info.bloqueadoHasta)) {
            info.bloqueadoHasta = null;
            info.noComprensiones = 0;
            return false;
        }
        return true;
    }

    public boolean estaCooldown(String ip) {
        IpInfo info = ipInfoMap.get(ip);
        if (info == null || info.cooldownHasta == null) return false;
        if (Instant.now().isAfter(info.cooldownHasta)) {
            info.cooldownHasta = null;
            return false;
        }
        return true;
    }

    public long segundosRestantes(String ip) {
        IpInfo info = ipInfoMap.get(ip);
        if (info == null) return 0;
        // Bloqueo tiene prioridad sobre cooldown
        Instant hasta = info.bloqueadoHasta != null ? info.bloqueadoHasta : info.cooldownHasta;
        if (hasta == null) return 0;
        return Math.max(0, Duration.between(Instant.now(), hasta).getSeconds());
    }

    public void registrarFarewell(String ip) {
        IpInfo info = ipInfoMap.computeIfAbsent(ip, k -> new IpInfo());
        info.noComprensiones++;
        info.cooldownHasta = Instant.now().plus(COOLDOWN_FAREWELL);
        log.info("Chatbot - IP {} acumuló {} mensaje(s) incomprensible(s)", ip, info.noComprensiones);
        if (info.noComprensiones >= MAX_NO_COMPRENSIONES) {
            info.bloqueadoHasta = Instant.now().plus(BLOQUEO_DURACION);
            log.warn("Chatbot - IP {} bloqueada por {} horas", ip, BLOQUEO_DURACION.toHours());
        }
    }

    public void registrarMensajeNormal(String ip) {
        IpInfo info = ipInfoMap.get(ip);
        if (info != null) {
            info.noComprensiones = 0;
            info.cooldownHasta = null;
        }
    }

    // clave = IP para el chat del sitio, autorId (o commentId) para los bots de Facebook/Instagram
    // -- el mapa es el mismo, ambos canales comparten el mismo contador y ventana por clave.
    public boolean limiteMensajesExcedido(String clave) {
        IpInfo info = ipInfoMap.get(clave);
        if (info == null || info.ventanaInicio == null) return false;
        if (Instant.now().isAfter(info.ventanaInicio.plus(VENTANA_LIMITE))) {
            return false;
        }
        return info.mensajesEnVentana >= MAX_MENSAJES_POR_HORA;
    }

    public void registrarMensaje(String clave) {
        IpInfo info = ipInfoMap.computeIfAbsent(clave, k -> new IpInfo());
        if (info.ventanaInicio == null || Instant.now().isAfter(info.ventanaInicio.plus(VENTANA_LIMITE))) {
            info.ventanaInicio = Instant.now();
            info.mensajesEnVentana = 0;
        }
        info.mensajesEnVentana++;
    }

    public long segundosRestantesLimite(String clave) {
        IpInfo info = ipInfoMap.get(clave);
        if (info == null || info.ventanaInicio == null) return 0;
        return Math.max(0, Duration.between(Instant.now(), info.ventanaInicio.plus(VENTANA_LIMITE)).getSeconds());
    }

    private static class IpInfo {
        int noComprensiones = 0;
        Instant bloqueadoHasta = null;
        Instant cooldownHasta = null;
        int mensajesEnVentana = 0;
        Instant ventanaInicio = null;
    }
}