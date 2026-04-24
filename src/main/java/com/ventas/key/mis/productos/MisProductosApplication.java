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

        String user = Base64.getEncoder().encodeToString("user_ventas".getBytes());
        String password = Base64.getEncoder().encodeToString("Luvianos#130594".getBytes());
        String urlCOnetct = Base64.getEncoder().encodeToString("51.178.29.99".getBytes());

        log.info("info user {} pass {}",user, password);
		SpringApplication.run(MisProductosApplication.class, args);
	}

}
