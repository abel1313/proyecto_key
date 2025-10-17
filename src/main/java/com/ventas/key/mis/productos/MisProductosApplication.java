package com.ventas.key.mis.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MisProductosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MisProductosApplication.class, args);
	}

}
