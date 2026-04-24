package com.ventas.key.mis.productos.security;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${api.cors_angular}")
    private String corsAngular;
    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Se ejecutan los cors desde security {}",corsAngular);
        return http        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // habilita CORS con tu bean
        .csrf(AbstractHttpConfigurer::disable) // desactiva CSRF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/registrar", "/auth/refresh", "/auth/logout", "/auth/validar", "/productos/getProductos2/**", "/imagen/**", "/swagger-ui/**","/dipomex/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/pagos/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/pedidos/**").authenticated()
                        .requestMatchers("/ventas/**","/rifa/**","/gastos/**","/ganadorRifa/**","/configurarRifa/**","/concursante/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/pedidos/**").hasRole("ADMIN")
                        .requestMatchers( HttpMethod.POST,"/clientes/**","/pedidos/**", "/variantes/buscar","/usuarios/buscarClientePorIdUsuario").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "https://shop.novedades-jade.com.mx",
        "https://front.novedades-jade.com.mx",
        "http://localhost:4200",
        "http://51.178.29.99:30001",
        "https://venta-bolsas-online.netlify.app",
        "https://novedades-jade.com.mx",
        "https://www.novedades-jade.com.mx"
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*")); // acepta todos los headers
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
}
