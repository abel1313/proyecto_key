package com.ventas.key.mis.productos.models;


public interface ICrud<
                Request,
                Response,
                ListResponse, 
                ResponseOptional, 
                TiopoDato> {

    Response save(Request req) throws Exception;
    ResponseOptional findById(TiopoDato tipo) throws Exception;
    ListResponse findAll(int pagina, int size) throws Exception;
    Response delete(TiopoDato tipo) throws Exception;
}
