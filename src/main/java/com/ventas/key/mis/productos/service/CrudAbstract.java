package com.ventas.key.mis.productos.service;

import java.util.List;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.repository.BaseRepository;

public abstract class CrudAbstract<Request,
                                    Response,
                                    ListResponse extends List<Response>, 
                                    ResponseOptional extends Optional<Response>, 
                                    TiopoDato>
                                    implements ICrud<
                                    Request,
                                    Response,
                                    ListResponse, 
                                    ResponseOptional, 
                                    TiopoDato> {
    protected final BaseRepository<Response,TiopoDato> repoGenerico;
    public CrudAbstract(
        final BaseRepository<Response,TiopoDato> repoGenerico
    ){
        this.repoGenerico = repoGenerico;
    }

    @Override
    public Response delete(TiopoDato tipo) throws Exception {
    // TODO Auto-generated method stub
    return null;
    }

    @Override
    public ResponseOptional findById(TiopoDato tipo) throws Exception {
    // TODO Auto-generated method stub
    return (ResponseOptional)repoGenerico.findById(tipo);
    }

    @Override
    public Response save(Request req) throws Exception {
    // TODO Auto-generated method stub
    return null;
    }

    @Override
    public ListResponse findAll(int pagina, int size) throws Exception {
    // TODO Auto-generated method stub
    return (ListResponse)repoGenerico.findAll();
    }



    

}
