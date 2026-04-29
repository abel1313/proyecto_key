package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.chatbot.ChatbotRequest;
import com.ventas.key.mis.productos.chatbot.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/mensaje")
    public ResponseEntity<Map<String, String>> enviarMensaje(@Valid @RequestBody ChatbotRequest request) {
        log.info("Chatbot recibió mensaje: {}", request.getMensaje());
        String respuesta = chatbotService.chat(request);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }
}