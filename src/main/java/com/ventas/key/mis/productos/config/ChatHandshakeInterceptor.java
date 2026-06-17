package com.ventas.key.mis.productos.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String forwarded = servletRequest.getServletRequest().getHeader("X-Forwarded-For");
            String ip = (forwarded != null && !forwarded.isBlank())
                    ? forwarded.split(",")[0].trim()
                    : servletRequest.getServletRequest().getRemoteAddr();
            attributes.put("clientIp", ip);
            log.info("[WS] Handshake aceptado — ip={}, origin={}", ip,
                    servletRequest.getServletRequest().getHeader("Origin"));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
