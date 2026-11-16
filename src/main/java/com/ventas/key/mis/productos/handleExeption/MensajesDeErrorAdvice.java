package com.ventas.key.mis.productos.handleExeption;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Ultima red antes de que una respuesta de error salga al front: si el mensaje trae texto tecnico (SQL, Hibernate, Java) se cambia por uno que
 * entienda quien usa la pantalla. El mensaje original queda en el log. Cubre tambien los controladores que arman su propio error con
 * {@code e.getMessage()}. Solo toca respuestas con estado 400 o mas; las exitosas no se tocan.
 */
@ControllerAdvice
@Slf4j
public class MensajesDeErrorAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResponseGeneric<?> r && r.getMensaje() != null && estadoDeError(response) && MensajesParaUsuario.esTecnico(r.getMensaje())) {
            log.warn("Mensaje tecnico traducido para el usuario ({} {}): {}", request.getMethod(), request.getURI().getPath(), r.getMensaje());
            r.setMensaje(MensajesParaUsuario.paraUsuario(r.getMensaje()));
        }
        return body;
    }

    private static boolean estadoDeError(ServerHttpResponse response) {
        return response instanceof ServletServerHttpResponse s && s.getServletResponse().getStatus() >= 400;
    }
}
