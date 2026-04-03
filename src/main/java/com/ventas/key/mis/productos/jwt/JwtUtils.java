//package com.ventas.key.mis.productos.jwt;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Date;
//import java.util.List;
//
//@Configuration
//public class JwtUtils {
//
//    @Value("${clave-seguridad.clave}")
//    private String secret;
//
//    private String secretKey;
//
//    @PostConstruct
//    public void initJwt() {
//        secretKey = secret;
//    }
//
//    private Key getSecretKey() {
//        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
//    }
//
//    public String obtenerUsuario(String token) {
//        return Jwts.parser()
//                .setSigningKey(getSecretKey())
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//
//
//    public  String generarToken(String usuario, List<String> roles) {
//        return Jwts.builder()
//                .setSubject(usuario) // usuario principal
//                .claim("roles", roles) // agregar roles como claim
//                .setIssuedAt(new Date()) // fecha de emisión
//                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // expira en 1 hora
//                .signWith(getSecretKey()) // firma segura
//                .compact();
//    }
//
//    public  List<String> obtenerRoles(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(getSecretKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .get("roles", List.class);
//    }
//
//
//    public Claims validarToken(String token) {
//        return Jwts.parserBuilder() .setSigningKey(getSecretKey()) .build() .parseClaimsJws(token) .getBody();
//    }
//}
