package com.ventas.key.mis.productos.errores;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ErrorGenerico {

    public void error(Throwable throwable ){
        if (throwable == null) return;
        log.error("Error no controlado: {}", throwable.getMessage(), throwable);
    }
}
