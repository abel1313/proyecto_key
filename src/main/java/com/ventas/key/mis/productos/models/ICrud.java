package com.ventas.key.mis.productos.models;

import java.util.List;
import java.util.Optional;

public interface ICrud<
                Response,
                ListResponse extends List<Response>, 
                ResponseOptional extends Optional<Response>, 
                TiopoDato,
                Paginacion extends PginaDto<List<Response>>
                > {

    Response save(Response req) throws Exception;
    ResponseOptional findById(TiopoDato tipo) throws Exception;
    ListResponse findAll(int pagina, int size) throws Exception;
    Response delete(TiopoDato tipo) throws Exception;

    Paginacion findAllNew(int pagina, int size) throws Exception;


}
