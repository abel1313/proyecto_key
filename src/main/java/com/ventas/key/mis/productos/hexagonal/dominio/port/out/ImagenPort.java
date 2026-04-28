package com.ventas.key.mis.productos.hexagonal.dominio.port.out;

import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;

import java.util.List;

public interface ImagenPort {

    List<ImagenDto> save(MultiValueMap<String, ?> multipartData);
    List<ImagenDto> getAll(List<Long> ids);
    ImagenDto getOne(Long id);
}
