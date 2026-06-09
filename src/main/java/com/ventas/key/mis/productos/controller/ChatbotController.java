package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import com.ventas.key.mis.productos.chatbot.ChatbotRequest;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Chatbot", description = "Asistente virtual de la tienda Novedades Jade con control de abuso por IP")
@RestController
@RequestMapping("/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;

    @Operation(
        summary = "Enviar mensaje al chatbot",
        description = "Procesa un mensaje del usuario y devuelve respuesta del asistente. " +
            "Control de abuso: si la IP esta bloqueada (tras despedidas o errores repetidos) devuelve bloqueado=true y segundosEspera>0. " +
            "Si la IP esta en cooldown entre mensajes devuelve bloqueado=false y segundosEspera>0. " +
            "Respuesta incluye: respuesta (String), bloqueado (boolean), segundosEspera (long)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Respuesta del chatbot; puede incluir indicador de bloqueo"),
        @ApiResponse(responseCode = "400", description = "Mensaje vacio o invalido"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/mensaje")
    public Mono<ResponseEntity<Map<String, Object>>> enviarMensaje(
            @Valid @RequestBody ChatbotRequest request,
            HttpServletRequest httpRequest) {

        String ip = obtenerIp(httpRequest);
        log.info("Chatbot - IP: {}, longitud mensaje: {}", ip, request.getMensaje().length());

        // --- IP bloqueada por 30 horas ---
        if (blockService.estaBloqueado(ip)) {
            long segs = blockService.segundosRestantes(ip);
            Map<String, Object> result = new HashMap<>();
            result.put("respuesta", "El chat no está disponible en este momento. "
                    + "Estará disponible en " + formatearTiempo(segs) + ". ¡Hasta pronto!");
            result.put("bloqueado", true);
            result.put("segundosEspera", segs);
            return Mono.just(ResponseEntity.ok(result));
        }

        // --- Cooldown entre mensajes incomprensibles ---
        if (blockService.estaCooldown(ip)) {
            long segs = blockService.segundosRestantes(ip);
            Map<String, Object> result = new HashMap<>();
            result.put("respuesta", "Por favor espera un momento antes de continuar.");
            result.put("bloqueado", false);
            result.put("segundosEspera", segs);
            return Mono.just(ResponseEntity.ok(result));
        }

        return chatbotService.chat(request)
                .map(respuesta -> {
                    boolean esFarewell = respuesta.contains("##FAREWELL##");
                    String respuestaLimpia = respuesta.replace("##FAREWELL##", "").trim();
                    Map<String, Object> result = new HashMap<>();

                    if (esFarewell) {
                        blockService.registrarFarewell(ip);
                        long segs = blockService.segundosRestantes(ip);
                        boolean ahoraBloqueado = blockService.estaBloqueado(ip);
                        String texto = ahoraBloqueado
                                ? respuestaLimpia + "\n\nEl chat estará disponible nuevamente en " + formatearTiempo(segs) + "."
                                : respuestaLimpia;
                        result.put("respuesta", texto);
                        result.put("bloqueado", ahoraBloqueado);
                        result.put("segundosEspera", segs);
                    } else {
                        blockService.registrarMensajeNormal(ip);
                        result.put("respuesta", respuestaLimpia);
                        result.put("bloqueado", false);
                        result.put("segundosEspera", 0);
                    }
                    return ResponseEntity.<Map<String, Object>>ok(result);
                })
                .onErrorResume(e -> {
                    // Timeout o error de OpenAI — cuenta como evento de bloqueo
                    log.error("Chatbot error IP {}: {}", ip, e.getMessage());
                    blockService.registrarFarewell(ip);
                    long segs = blockService.segundosRestantes(ip);
                    boolean ahoraBloqueado = blockService.estaBloqueado(ip);
                    Map<String, Object> result = new HashMap<>();
                    String mensaje = ahoraBloqueado
                            ? "El chat no está disponible en este momento. Estará disponible en " + formatearTiempo(segs) + "."
                            : "No pude procesar tu mensaje en este momento. Por favor intenta más tarde.";
                    result.put("respuesta", mensaje);
                    result.put("bloqueado", ahoraBloqueado);
                    result.put("segundosEspera", segs);
                    return Mono.just(ResponseEntity.<Map<String, Object>>ok(result));
                });
    }

    private String obtenerIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String formatearTiempo(long segundos) {
        long horas = segundos / 3600;
        long mins = (segundos % 3600) / 60;
        return horas > 0
                ? horas + " hora(s) y " + mins + " minuto(s)"
                : mins + " minuto(s)";
    }
}
