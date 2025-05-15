package com.ventas.key.mis.productos.service;

import java.util.List;

import com.ventas.key.mis.productos.models.IEjemplo;
import com.ventas.key.mis.productos.models.PginaDto;

public abstract class CrudRepo2<Response, Paginacion extends PginaDto<List<Response>>>
        implements IEjemplo<Response, Paginacion> {

    @Override
    public Paginacion findAllNew(int pagina, int size) throws Exception{
        PginaDto<List<Response>> paginacion = new PginaDto<>();
        

        return null;
    }

}

