package com.ventas.key.hexagonal.busquedapedido.infraestructura.cache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Set;

/**
 * [Hexagonal: Driving Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R18, red de seguridad: cualquier peticion que escribe (POST, PUT, PATCH, DELETE) y sale bien sube
 * la version de las listas de Mis pedidos al terminar, por si cambio algo con SQL directo que el
 * listener de JPA no ve. Se excluyen solo las que seguro no tocan pedidos y pasan a cada rato: guardar
 * los filtros (se guarda en cada clic de filtro y si no, el cache no serviria nunca), entrar o renovar la
 * sesion y el chatbot.
 */
@Configuration
@RequiredArgsConstructor
public class EscriturasWebInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final Set<String> LEEN = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final List<String> NO_TOCAN_PEDIDOS = List.of("/v1/preferencias-filtro", "/auth/", "/chatbot/");

    private final VersionDeListasDePedidos version;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || response.getStatus() >= 400 || LEEN.contains(request.getMethod())) {
            return;
        }
        String ruta = request.getRequestURI();
        if (NO_TOCAN_PEDIDOS.stream().anyMatch(ruta::contains)) {
            return;
        }
        version.subir(request.getMethod() + " " + ruta);
    }
}
