package com.ventas.key.mis.productos.exeption;

/**
 * Otro servicio del que dependemos (micro de imágenes, Meta, TikTok…) falló o no contestó.
 * ExceptionGlobal la responde con 502: el error no es de los datos que mandó el usuario, así que
 * reintentar más tarde puede funcionar.
 */
public class ExceptionServicioExterno extends RuntimeException {

    public ExceptionServicioExterno(String mensaje) {
        super(mensaje);
    }
}
