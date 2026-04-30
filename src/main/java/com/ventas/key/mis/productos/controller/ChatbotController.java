package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotRequest;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;

    /**
     * Respuesta:
     *   respuesta     (String)  - texto a mostrar al usuario
     *   bloqueado     (boolean) - true si la IP está bloqueada 6 h
     *   segundosEspera (long)   - segundos que el frontend debe deshabilitar el input
     */
    @PostMapping("/mensaje")
    public ResponseEntity<Map<String, Object>> enviarMensaje(
            @Valid @RequestBody ChatbotRequest request,
            HttpServletRequest httpRequest) {

        String ip = obtenerIp(httpRequest);
        log.info("Chatbot - IP: {}, mensaje: {}", ip, request.getMensaje());

        Map<String, Object> result = new HashMap<>();

        // --- IP bloqueada por 6 horas ---
        if (blockService.estaBloqueado(ip)) {
            long segs = blockService.segundosRestantes(ip);
            long horas = segs / 3600;
            long mins = (segs % 3600) / 60;
            String tiempo = horas > 0
                    ? horas + " hora(s) y " + mins + " minuto(s)"
                    : mins + " minuto(s)";
            result.put("respuesta", "El chat no está disponible en este momento. "
                    + "Estará disponible en " + tiempo + ". ¡Hasta pronto!");
            result.put("bloqueado", true);
            result.put("segundosEspera", segs);
            return ResponseEntity.ok(result);
        }

        // --- Cooldown entre mensajes incomprensibles ---
        if (blockService.estaCooldown(ip)) {
            long segs = blockService.segundosRestantes(ip);
            result.put("respuesta", "Por favor espera un momento antes de continuar.");
            result.put("bloqueado", false);
            result.put("segundosEspera", segs);
            return ResponseEntity.ok(result);
        }

        String respuesta = chatbotService.chat(request);
        boolean esFarewell = respuesta.contains("##FAREWELL##");
        respuesta = respuesta.replace("##FAREWELL##", "").trim();

        if (esFarewell) {
            blockService.registrarFarewell(ip);
            long segs = blockService.segundosRestantes(ip);
            boolean ahoraBloqueado = blockService.estaBloqueado(ip);
            if (ahoraBloqueado) {
                long horas = segs / 3600;
                long mins = (segs % 3600) / 60;
                String tiempo = horas > 0
                        ? horas + " hora(s) y " + mins + " minuto(s)"
                        : mins + " minuto(s)";
                respuesta += "\n\nEl chat estará disponible nuevamente en " + tiempo + ".";
            }
            result.put("respuesta", respuesta);
            result.put("bloqueado", ahoraBloqueado);
            result.put("segundosEspera", segs);
        } else {
            blockService.registrarMensajeNormal(ip);
            result.put("respuesta", respuesta);
            result.put("bloqueado", false);
            result.put("segundosEspera", 0);
        }

        return ResponseEntity.ok(result);
    }

    private String obtenerIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}