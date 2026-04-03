package com.ventas.key.mis.productos.hexagonal.aplicacion;

import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;

import java.util.List;

public interface ImagenesCasoUso {

    List<Imagen> create(List<Imagen> imagenes);
    List<Imagen> update(List<Imagen> imagenes);
    void delete(Long id);
    List<Imagen> getAll(List<Long> ids);

}
