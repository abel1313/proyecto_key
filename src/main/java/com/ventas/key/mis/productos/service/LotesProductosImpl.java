package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.service.api.ILoteProductoService;

@Service
public class LotesProductosImpl extends CrudAbstractServiceImpl<
                                                LotesProductos,
                                                List<LotesProductos>, 
                                                Optional<LotesProductos>, 
                                                Integer,
                                                PginaDto<List<LotesProductos>>> implements ILoteProductoService {

        private final ILostesProductosRepository iRepository;
        private final ErrorGenerico error;

        public LotesProductosImpl(
            final ILostesProductosRepository iRepository,
            final ErrorGenerico error
            ){
            super(iRepository,error);
            this.iRepository = iRepository;
            this.error = error;
        }

}
