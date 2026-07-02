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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag(name = "Chatbot", description = "Asistente virtual de la tienda Novedades Jade con control de abuso por IP")
@RestController
@RequestMapping("/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotBlockService blockService;

    private static final Pattern PRECIO_PATTERN = Pattern.compile("\\$\\s?\\d");
    private static final Pattern MENCIONA_FOTO_PATTERN =
            Pattern.compile("\\b(foto|fotos|imagen|imágenes)\\b", Pattern.CASE_INSENSITIVE);

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
                .flatMap(respuesta -> {
                    boolean tieneBuscarInicial = respuesta.contains("##BUSCAR[");
                    boolean pideImagen = MENCIONA_FOTO_PATTERN.matcher(request.getMensaje()).find();
                    if (!tieneBuscarInicial && pideImagen) {
                        log.warn("Chatbot no uso ##BUSCAR## pese a pedido de imagen (IP {}), reintentando", ip);
                        return chatbotService.forzarMostrarImagen(request, respuesta);
                    }
                    return Mono.just(respuesta);
                })
                .map(respuesta -> {
                    boolean esFarewell = respuesta.contains("##FAREWELL##");
                    // Extraer ##BUSCAR[query,offset]## si existe
                    Pattern buscarPattern = Pattern.compile("##BUSCAR\\[([^,\\]]+),?(\\d*)\\]##");
                    Matcher buscarMatcher = buscarPattern.matcher(respuesta);
                    boolean tieneBuscar = buscarMatcher.find();
                    String buscarQuery = tieneBuscar ? buscarMatcher.group(1).trim() : null;
                    int buscarOffset = (tieneBuscar && !buscarMatcher.group(2).isBlank())
                            ? Integer.parseInt(buscarMatcher.group(2)) : 0;

                    String respuestaLimpia = respuesta
                            .replace("##FAREWELL##", "")
                            .replaceAll("##BUSCAR\\[[^\\]]*\\]##", "")
                            .trim();

                    // Red de seguridad: si el bot confirmó un producto (menciona precio) pero no
                    // mostró imagen ni ofreció mostrarla, el cliente no tiene forma de saber que
                    // puede pedir la foto. El modelo debería preguntarlo solo (CASO 1 del prompt),
                    // pero no siempre lo hace — se fuerza aquí para no depender de eso.
                    if (!tieneBuscar && !esFarewell
                            && PRECIO_PATTERN.matcher(respuestaLimpia).find()
                            && !MENCIONA_FOTO_PATTERN.matcher(respuestaLimpia).find()) {
                        respuestaLimpia = respuestaLimpia + "\n\n¿Quieres ver una foto? 📸";
                    }

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

                    if (tieneBuscar && buscarQuery != null && !buscarQuery.isBlank()) {
                        Map<String, Object> busqueda = chatbotService.buscarProductos(buscarQuery, buscarOffset);
                        result.put("productos", busqueda.get("productos"));
                        result.put("hayMas", busqueda.get("hayMas"));
                        result.put("busquedaQuery", busqueda.get("busquedaQuery"));
                        result.put("busquedaOffset", busqueda.get("busquedaOffset"));
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

    @Operation(
        summary = "Ver más productos del chatbot",
        description = "Paginación de productos sin llamar a la IA. Usar cuando el usuario hace clic en 'Ver más'."
    )
    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscar(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(chatbotService.buscarProductos(q, offset));
    }

    private String obtenerIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // IP más a la derecha = la que agregó el proxy de confianza (nginx).
            // La primera puede ser falsa si el cliente forjó el header.
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
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
