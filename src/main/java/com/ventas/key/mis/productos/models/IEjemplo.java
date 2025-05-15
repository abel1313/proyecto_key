package com.ventas.key.mis.productos.models;

import java.util.List;

public interface IEjemplo<Response, Paginacion extends PginaDto<List<Response>>> {
    Paginacion findAllNew(int pagina, int size) throws Exception;
}

