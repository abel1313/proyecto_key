package com.ventas.key.mis.productos.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Filtro global de saneamiento de entradas (2026-09-04) -- ver rama feature/filtro-seguridad.
 * Corre ANTES de {@link JwtAuthenticationFilter}, asi que revisa TODAS las peticiones front->back,
 * publicas y autenticadas por igual, antes de que lleguen a un controlador.
 *
 * Que revisa: URL (path), query params, y el body -- si es JSON se recorre recursivamente cada
 * valor string; si es multipart, solo los CAMPOS DE TEXTO (los archivos binarios de un multipart
 * NUNCA se inspeccionan aca -- es un problema distinto, de validacion de tipo de archivo, no de
 * este filtro).
 *
 * Que hace si encuentra algo sospechoso: RECHAZA toda la peticion con 400 (decision explicita del
 * usuario -- no sanear-y-continuar). Las 4 categorias que busca (ver DetectorPatronesMaliciosos):
 * XSS, inyeccion SQL, path traversal, bytes nulos/caracteres de control. Todos los patrones son
 * combinaciones/firmas -- nunca un caracter suelto (una comilla o un guion solos no disparan nada).
 */
@Component
@Slf4j
public class InputSanitizationFilter extends OncePerRequestFilter {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // El preflight de CORS no trae datos de negocio -- dejarlo pasar igual que en SecurityConfig.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Hallazgo> hallazgo = revisarPath(request);
        if (hallazgo.isEmpty()) {
            hallazgo = revisarQueryYFormParams(request);
        }

        boolean esMultipart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/");

        HttpServletRequest requestParaSiguienteFiltro = request;

        if (hallazgo.isEmpty() && esMultipart) {
            hallazgo = revisarPartesDeTextoMultipart(request);
        } else if (hallazgo.isEmpty() && tieneCuerpo(request)) {
            CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);
            requestParaSiguienteFiltro = cached;
            hallazgo = revisarCuerpo(cached, request.getContentType());
        }

        if (hallazgo.isPresent()) {
            rechazar(request, response, hallazgo.get());
            return;
        }

        filterChain.doFilter(requestParaSiguienteFiltro, response);
    }

    private boolean tieneCuerpo(HttpServletRequest request) {
        return request.getContentLengthLong() > 0
                || "chunked".equalsIgnoreCase(request.getHeader("Transfer-Encoding"));
    }

    // ── URL / path ────────────────────────────────────────────────────
    private Optional<Hallazgo> revisarPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Optional<String> categoria = DetectorPatronesMaliciosos.categoriaSospechosa(uri);
        if (categoria.isPresent()) {
            return Optional.of(new Hallazgo(categoria.get(), "path", uri));
        }
        // Tambien la version decodificada -- alguien puede mandar %2e%2e%2f ya semi-decodificado
        // una vez por el navegador y el filtro necesita verlo tal cual llegaria al SO/disco.
        try {
            String decodificada = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            categoria = DetectorPatronesMaliciosos.categoriaSospechosa(decodificada);
            if (categoria.isPresent()) {
                return Optional.of(new Hallazgo(categoria.get(), "path", decodificada));
            }
        } catch (IllegalArgumentException ignorado) {
            // URL mal formada -- no es este filtro quien decide eso, sigue su curso normal.
        }
        return Optional.empty();
    }

    // ── Query params + form params urlencoded ────────────────────────
    private Optional<Hallazgo> revisarQueryYFormParams(HttpServletRequest request) {
        if (request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/")) {
            // getParameterMap() en un multipart puede disparar el parseo de partes antes de
            // tiempo -- ese caso ya lo cubre revisarPartesDeTextoMultipart.
            return Optional.empty();
        }
        Map<String, String[]> parametros = request.getParameterMap();
        for (Map.Entry<String, String[]> entrada : parametros.entrySet()) {
            Optional<String> categoriaClave = DetectorPatronesMaliciosos.categoriaSospechosa(entrada.getKey());
            if (categoriaClave.isPresent()) {
                return Optional.of(new Hallazgo(categoriaClave.get(), "query-param(clave)", entrada.getKey()));
            }
            for (String valor : entrada.getValue()) {
                Optional<String> categoria = DetectorPatronesMaliciosos.categoriaSospechosa(valor);
                if (categoria.isPresent()) {
                    return Optional.of(new Hallazgo(categoria.get(), "query-param:" + entrada.getKey(), valor));
                }
            }
        }
        return Optional.empty();
    }

    // ── Multipart: solo campos de texto, NUNCA el binario de un archivo ──
    private Optional<Hallazgo> revisarPartesDeTextoMultipart(HttpServletRequest request) {
        try {
            for (Part part : request.getParts()) {
                boolean esArchivo = part.getSubmittedFileName() != null;
                if (esArchivo) {
                    continue; // contenido binario -- fuera de alcance de este filtro a proposito
                }
                String valor = StreamUtils.copyToString(part.getInputStream(), StandardCharsets.UTF_8);
                Optional<String> categoria = DetectorPatronesMaliciosos.categoriaSospechosa(valor);
                if (categoria.isPresent()) {
                    return Optional.of(new Hallazgo(categoria.get(), "multipart-campo:" + part.getName(), valor));
                }
            }
        } catch (IOException | ServletException e) {
            // No se pudo parsear el multipart aca -- se deja pasar sin bloquear (el resto de la
            // seguridad -- auth, validacion de tipo de archivo -- sigue aplicando rio abajo); se
            // deja registrado para no perder visibilidad de que este filtro no llego a revisarlo.
            log.warn("InputSanitizationFilter no pudo leer las partes del multipart en {}: {}",
                    request.getRequestURI(), e.getMessage());
        }
        return Optional.empty();
    }

    // ── Body no-multipart: si es JSON se recorre recursivamente, si no se revisa como texto plano ──
    private Optional<Hallazgo> revisarCuerpo(CachedBodyHttpServletRequest cached, String contentType) {
        byte[] bytes = cached.getCuerpoCacheado();
        if (bytes.length == 0) {
            return Optional.empty();
        }
        String texto = new String(bytes, StandardCharsets.UTF_8);
        if (texto.isBlank()) {
            return Optional.empty();
        }

        boolean pareceJson = contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
        if (pareceJson) {
            try {
                JsonNode raiz = objectMapper.readTree(texto);
                return revisarNodoJson(raiz, "body");
            } catch (IOException noEsJsonValido) {
                // Cae al escaneo de texto plano de abajo -- si ni siquiera es JSON valido, que lo
                // rechace igual el binder de Jackson mas adelante; este filtro solo revisa firmas.
            }
        }

        Optional<String> categoria = DetectorPatronesMaliciosos.categoriaSospechosa(texto);
        if (categoria.isPresent()) {
            return Optional.of(new Hallazgo(categoria.get(), "body", texto));
        }
        return Optional.empty();
    }

    private Optional<Hallazgo> revisarNodoJson(JsonNode nodo, String ruta) {
        if (nodo.isTextual()) {
            Optional<String> categoria = DetectorPatronesMaliciosos.categoriaSospechosa(nodo.asText());
            if (categoria.isPresent()) {
                return Optional.of(new Hallazgo(categoria.get(), ruta, nodo.asText()));
            }
            return Optional.empty();
        }
        if (nodo.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> campos = nodo.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                Optional<String> categoriaClave = DetectorPatronesMaliciosos.categoriaSospechosa(campo.getKey());
                if (categoriaClave.isPresent()) {
                    return Optional.of(new Hallazgo(categoriaClave.get(), ruta + "." + campo.getKey() + "(clave)", campo.getKey()));
                }
                Optional<Hallazgo> hallazgo = revisarNodoJson(campo.getValue(), ruta + "." + campo.getKey());
                if (hallazgo.isPresent()) {
                    return hallazgo;
                }
            }
            return Optional.empty();
        }
        if (nodo.isArray()) {
            for (int i = 0; i < nodo.size(); i++) {
                Optional<Hallazgo> hallazgo = revisarNodoJson(nodo.get(i), ruta + "[" + i + "]");
                if (hallazgo.isPresent()) {
                    return hallazgo;
                }
            }
        }
        return Optional.empty();
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response, Hallazgo hallazgo) throws IOException {
        String muestra = hallazgo.valor().length() > 200 ? hallazgo.valor().substring(0, 200) + "..." : hallazgo.valor();
        log.warn("InputSanitizationFilter rechazo {} {} -- categoria={} ubicacion={} valor='{}'",
                request.getMethod(), request.getRequestURI(), hallazgo.categoria(), hallazgo.ubicacion(), muestra);

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                new ResponseGeneric<>(null, "La peticion contiene datos no permitidos y fue rechazada")));
    }

    private record Hallazgo(String categoria, String ubicacion, String valor) {
    }
}
