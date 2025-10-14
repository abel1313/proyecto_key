package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoImagenServiceImpl extends CrudAbstractServiceImpl<
                                                ProductoImagen,
                                                List<ProductoImagen>,
                                                Optional<ProductoImagen>,
                                                Integer,
                                                PginaDto<List<ProductoImagen>>> implements IProductoImagenService {

    private final IProductoImagenRepository iProductoImagenRepository;
    public ProductoImagenServiceImpl(BaseRepository<ProductoImagen, Integer> repoGenerico, ErrorGenerico error,
                                     final IProductoImagenRepository iProductoImagenRepository) {
        super(repoGenerico, error);
        this.iProductoImagenRepository = iProductoImagenRepository;
    }

    @Override
    public List<ProductoImagen> saveAll(List<ProductoImagen> productoImagen) {
        return this.iProductoImagenRepository.saveAll(productoImagen);
    }

    @Override
    public List<ProductoImagen> findByProductoId(Integer productoId) {
        return this.iProductoImagenRepository.findByProductoId(productoId);
    }
}
