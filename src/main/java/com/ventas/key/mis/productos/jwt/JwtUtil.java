package com.ventas.key.mis.productos.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtil {

    @Value("${clave-seguridad.clave}")
    private String secret;


    private String secretKey;

    @PostConstruct
    public void initJwt() {
        secretKey = secret;
    }
    private Key getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails, long idUsuarioRegistrado) {
        return generateToken(userDetails, idUsuarioRegistrado, List.of(), List.of(), List.of());
    }

    /**
     * @param pantallas rutas (Submenu.ruta) efectivas del usuario -- rol + sus excepciones (ver
     *                  UsuarioServiceImpl.submenusEfectivos). El front las usa para armar el menu
     *                  dinamico y el PantallaGuard sin tener que pedirlas al back en cada navegacion.
     *                  Se recalculan en cada login/refresh (cada 15 min como maximo), asi que un
     *                  cambio de permisos por el admin tarda como mucho eso en reflejarse.
     * @param pantallasEscritura subconjunto de {@code pantallas} en las que el usuario, ademas de
     *                  poder VERLAS, puede ESCRIBIR (crear/editar/borrar) -- Fase 2 de permisos
     *                  de accion (2026-08-27), ver UsuarioServiceImpl.submenusEscritura. El
     *                  backend ya lo exige via SecurityConfig.pantallaEscribir() sin importar este
     *                  claim; se manda ademas para que el front pueda, pantalla por pantalla,
     *                  mostrar un modo de solo lectura (ocultar/deshabilitar guardar-editar-borrar)
     *                  en vez de dejar que el usuario intente y se tope con un 403.
     * @param pantallasAcciones acciones puntuales dentro de una pantalla que el usuario puede usar
     *                  (Fase 3 de permisos, piloto en Modelos 2026-08-27), formato "ruta:clave"
     *                  (ej. "productos/buscar:eliminar") -- ver UsuarioServiceImpl.accionesEfectivas.
     *                  Mismo criterio que pantallasEscritura: el backend ya lo exige via
     *                  SecurityConfig.accion(), este claim es para que el front pueda mostrar u
     *                  ocultar cada boton puntual sin adivinar.
     */
    public String generateToken(UserDetails userDetails, long idUsuarioRegistrado, List<String> pantallas,
                                 List<String> pantallasEscritura, List<String> pantallasAcciones) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(mpa -> mpa.getAuthority())
                .collect(Collectors.toList()));
        claims.put("idUsuario", idUsuarioRegistrado);
        claims.put("pantallas", pantallas);
        claims.put("pantallasEscritura", pantallasEscritura);
        claims.put("pantallasAcciones", pantallasAcciones);
        return Jwts.builder()
                .setClaims(claims)
                .setId(java.util.UUID.randomUUID().toString())   // jti — para poder invalidarlo
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 minutos
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @param sessionStart epoch-millis del login original; se propaga en cada refresh
     *                     para poder calcular la duración absoluta de la sesión.
     * @param jti          identificador del token, la fila de {@code sesion_refresh} guarda el
     *                     único vigente. Es lo que permite invalidarlo del lado del servidor.
     * @param sessionId    familia de la sesión; no cambia entre rotaciones y sirve para localizar
     *                     la sesión aunque el jti ya haya rotado (detección de reuso).
     */
    public String generateRefreshToken(UserDetails userDetails, long idUsuarioRegistrado, long sessionStart,
                                       String jti, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("idUsuario", idUsuarioRegistrado);
        claims.put("type", "refresh");
        claims.put("sessionStart", sessionStart);
        claims.put("sessionId", sessionId);
        return Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7 días
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long extractSessionStart(String token) {
        Object val = Jwts.parserBuilder()
                .setSigningKey(getSecretKey()).build()
                .parseClaimsJws(token).getBody().get("sessionStart");
        if (val instanceof Number) return ((Number) val).longValue();
        return System.currentTimeMillis();
    }

    /** jti del refresh token — se compara contra el vigente en {@code sesion_refresh}. */
    public String extractJti(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey()).build()
                .parseClaimsJws(token).getBody().getId();
    }

    /**
     * Familia de la sesión. Devuelve null en los refresh tokens emitidos antes de que el refresh
     * pasara a ser stateful — esos ya no se pueden renovar y obligan a iniciar sesión de nuevo.
     */
    public String extractSessionId(String token) {
        Object val = Jwts.parserBuilder()
                .setSigningKey(getSecretKey()).build()
                .parseClaimsJws(token).getBody().get("sessionId");
        return val == null ? null : val.toString();
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = (String) Jwts.parserBuilder()
                    .setSigningKey(getSecretKey()).build()
                    .parseClaimsJws(token).getBody().get("type");
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    /** Fecha de emision (iat) del token — se compara contra Usuario.passwordActualizadoEn para rechazar access tokens viejos. */
    public Date extractIssuedAt(String token) {
        return Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token).getBody().getIssuedAt();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isRefreshToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

}
