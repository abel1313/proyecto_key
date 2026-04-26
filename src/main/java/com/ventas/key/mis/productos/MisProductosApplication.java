package com.ventas.key.mis.productos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@Slf4j
public class MisProductosApplication {

	public static void main(String[] args) {
		System.out.println("=== VARIABLES DE ENTORNO ===");
		System.out.println("PERFIL: " + System.getenv("SPRING_PROFILES_ACTIVE"));
		System.out.println("DB_HOST: " + System.getenv("DB_HOST"));
		System.out.println("SPRING_DB_NAME: " + System.getenv("SPRING_DB_NAME"));
		System.out.println("SPRING_DATASOURCE_USERNAME: " + System.getenv("SPRING_DATASOURCE_USERNAME"));
		System.out.println("SPRING_DB_NAME: " + System.getenv("SPRING_DB_NAME"));
		System.out.println("SPRING_DATASOURCE_USERNAME: " + System.getenv("SPRING_DATASOURCE_USERNAME"));
		System.out.println("============================");
		SpringApplication.run(MisProductosApplication.class, args);
	}
}
