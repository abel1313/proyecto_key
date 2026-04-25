package com.ventas.key.mis.productos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Base64;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@Slf4j
public class MisProductosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MisProductosApplication.class, args);
	}

}
