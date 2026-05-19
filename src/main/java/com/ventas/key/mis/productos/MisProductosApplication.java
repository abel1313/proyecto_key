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
		System.out.println("============================ se eliminaro los logs de variables de entono");

        String encri = Base64.getEncoder().encodeToString(("Luvianos *13").getBytes());
        String tokenKWT = Base64.getEncoder().encodeToString(("Token_Luvianos_Estado_de_Mexico *13").getBytes());
        log.info("tokenKWT {}",tokenKWT);
		SpringApplication.run(MisProductosApplication.class, args);
	}
}
