package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.service.api.IProductoService;

@Service
public class ProductosServiceImpl extends CrudAbstract<Producto,
                                                Producto,
                                                List<Producto>, 
                                                Optional<Producto>, 
                                                Integer>
                                                implements IProductoService{


                                                    
private final IProductosRepository iProductosRepository;
    public ProductosServiceImpl(final IProductosRepository iProductosRepository) {
                                                super(iProductosRepository);
                                                this.iProductosRepository = iProductosRepository;
                                                //TODO Auto-generated constructor stub
                                                }

    @Override
    public Producto actualizarStock(Integer id, Integer nuevoStock) {
    // TODO Auto-generated method stub
    return null;
    }





}
