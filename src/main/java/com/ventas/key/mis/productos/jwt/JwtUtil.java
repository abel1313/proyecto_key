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
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(mpa-> {
                    log.info("info {}",mpa);
                    return mpa.getAuthority();
                })
                .collect(Collectors.toList()));
        claims.put("idUsuario", idUsuarioRegistrado);
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
     */
    public String generateRefreshToken(UserDetails userDetails, long idUsuarioRegistrado, long sessionStart) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("idUsuario", idUsuarioRegistrado);
        claims.put("type", "refresh");
        claims.put("sessionStart", sessionStart);
        return Jwts.builder()
                .setClaims(claims)
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

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
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
