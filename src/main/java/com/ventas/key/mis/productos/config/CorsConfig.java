package com.ventas.key.mis.productos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer{

        @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "https://venta-bolsas-online.netlify.app","venta-bolsas-online.netlify.app","https://novedades-jade.com.mx", "https://www.novedades-jade.com.mx")
//                .allowedOrigins("http://localhost:4200", "https://venta-bolsas-online.netlify.app","https://novedades-jade.com.mx", "https://www.novedades-jade.com.mx")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }

}
