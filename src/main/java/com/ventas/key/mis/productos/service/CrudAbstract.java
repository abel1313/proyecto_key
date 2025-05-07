package com.ventas.key.mis.productos.service;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
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
    protected final ErrorGenerico error;
    public CrudAbstract(
        final BaseRepository<Response,TiopoDato> repoGenerico,
        final ErrorGenerico error
    ){
        this.repoGenerico = repoGenerico;
        this.error = error;
    }

    @Override
    public Response delete(TiopoDato tipo) throws Exception {
        try {
            
        } catch (Exception e) {
            error.error(e);
        }
    return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseOptional findById(TiopoDato tipo) throws Exception {

        try {

            return (ResponseOptional)repoGenerico.findById(tipo);
        } catch (Exception e) {
            error.error(e);
        }
        return (ResponseOptional) Optional.empty();
    }

    @Override
    public Response save(Request req) throws Exception {
    try {
        return this.save(req);
    } catch (Exception e) {
        error.error(e);
    }
    return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListResponse findAll(int pagina, int size) throws Exception {
        try {
        
        Pageable pageable2 = PageRequest.of(1, 1);

        Page<Response> productosPaginados = repoGenerico.findAll(pageable2);
        ListResponse listaProductos = (ListResponse)productosPaginados.getContent(); 

            return listaProductos;
        } catch (Exception e) {
            error.error(e);
        }
        return (ListResponse) new ArrayList<>();
    }
}
