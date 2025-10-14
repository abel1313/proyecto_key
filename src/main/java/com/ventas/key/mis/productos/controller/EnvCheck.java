package com.ventas.key.mis.productos.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EnvCheck implements CommandLineRunner {
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Override
    public void run(String... args) {
        System.out.println("DB URL cargado: " + dbUrl);
    }
}
