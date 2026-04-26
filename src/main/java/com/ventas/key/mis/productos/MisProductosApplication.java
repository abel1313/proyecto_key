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
		log.info("=== VARIABLES DE ENTORNO ===");
		log.info("PERFIL: {}", System.getenv("SPRING_PROFILES_ACTIVE"));
		log.info("DB_HOST: {}", System.getenv("DB_HOST"));
		log.info("SPRING_DB_NAME: {}", System.getenv("SPRING_DB_NAME"));
		log.info("SPRING_DATASOURCE_USERNAME: {}", System.getenv("SPRING_DATASOURCE_USERNAME"));
		log.info("SPRING_DB_NAME_QA: {}", System.getenv("SPRING_DB_NAME_QA"));
		log.info("SPRING_DATASOURCE_USERNAME_QA: {}", System.getenv("SPRING_DATASOURCE_USERNAME_QA"));
		log.info("============================");
		SpringApplication.run(MisProductosApplication.class, args);
	}
}
