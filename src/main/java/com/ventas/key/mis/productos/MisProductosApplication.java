package com.ventas.key.mis.productos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Base64;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@Slf4j
public class MisProductosApplication {

	public static void main(String[] args) {
        System.out.println("===========================================================");
        System.out.println("  proyecto_key — QA estable al 2026-05-23");
        System.out.println("  manifests K8s RabbitMQ, analisis migracion imagenes");
        System.out.println("===========================================================");
        String encri = Base64.getEncoder().encodeToString(("").getBytes());
		SpringApplication.run(MisProductosApplication.class, args);
	}
}
