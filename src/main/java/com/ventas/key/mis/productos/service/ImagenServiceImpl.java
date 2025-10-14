package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.ILoteProductoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ImagenServiceImpl extends CrudAbstractServiceImpl<
        Imagen,
        List<Imagen>,
        Optional<Imagen>,
        Integer,
        PginaDto<List<Imagen>>> implements IImagenService {
    private final IImagenRepository iImagenRepository;
    public ImagenServiceImpl(BaseRepository<Imagen, Integer> repoGenerico, ErrorGenerico error,
                             final IImagenRepository iImagenRepository) {
        super(repoGenerico, error);
        this.iImagenRepository = iImagenRepository;
    }


    @Override
    public List<Imagen> saveAll(List<Imagen> list) {
        return this.iImagenRepository.saveAll(list);
    }
}
