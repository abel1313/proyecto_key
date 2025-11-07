package com.ventas.key.mis.productos.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class Cacha {


    @CacheEvict(value = "imagenes", allEntries = true)
    public void limpiarTodaLaCacheDeImagenes() {

    }

//    @CacheEvict(value = "imagenes", allEntries = true)
//    public void limpiarTodaLaCacheDeImagenes() {
//
//    }

    @CacheEvict(value = "detalleImagen", allEntries = true)
    public void deleteByImg() {

    }
}
