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
    private static final Duration BLOQUEO_DURACION = Duration.ofHours(6);
    private static final Duration COOLDOWN_FAREWELL = Duration.ofSeconds(45);

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

    private static class IpInfo {
        int noComprensiones = 0;
        Instant bloqueadoHasta = null;
        Instant cooldownHasta = null;
    }
}