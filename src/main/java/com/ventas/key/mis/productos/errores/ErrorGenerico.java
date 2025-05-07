package com.ventas.key.mis.productos.errores;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ErrorGenerico {

    public void error(Throwable throwable ){
        if( throwable != null && 
            throwable.getStackTrace() != null &&
            throwable.getStackTrace()[0] != null )
        log.error("Ocurrio un error en la clase {} en el metodo {} error mensaje {} ", throwable.getStackTrace()[0].getClassName(), 
                                                                                        throwable.getStackTrace()[0].getMethodName(),
                                                                                        throwable.getMessage());
    }
}
