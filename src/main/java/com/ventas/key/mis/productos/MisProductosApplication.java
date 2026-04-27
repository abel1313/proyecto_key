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
		System.out.println("============================ se eliminaro los logs de variables de entono");
		SpringApplication.run(MisProductosApplication.class, args);
	}
}
