package com.ventas.key.mis.productos.hexagonal.dominio.service;

import com.ventas.key.mis.productos.hexagonal.aplicacion.ImagenesCasoUso;
import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImagenService implements ImagenesCasoUso {
    @Override
    public List<Imagen> create(List<Imagen> imagenes) {
        return null;
    }

    @Override
    public List<Imagen> update(List<Imagen> imagenes) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public List<Imagen> getAll(List<Long> ids) {
        return null;
    }
}
