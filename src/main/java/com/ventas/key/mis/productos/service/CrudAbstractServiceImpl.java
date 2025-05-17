package com.ventas.key.mis.productos.service;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;

public abstract class CrudAbstractServiceImpl<
                                    Response,
                                    ListResponse extends List<Response>, 
                                    ResponseOptional extends Optional<Response>, 
                                    TiopoDato,
                                    Paginacion extends PginaDto<List<Response>>>
                                    implements ICrud<
                                    Response,
                                    ListResponse, 
                                    ResponseOptional, 
                                    TiopoDato,
                                    Paginacion> {
    protected final BaseRepository<Response,TiopoDato> repoGenerico;
    protected final ErrorGenerico error;
    public CrudAbstractServiceImpl(
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
    public Response save(Response req) throws Exception {
    try {
        return (Response) this.repoGenerico.save(req);
    } catch (Exception e) {
        error.error(e);
    }
    return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListResponse findAll(int pagina, int size) throws Exception {
        try {
        
        Pageable pageable2 = PageRequest.of(pagina, size);

        Page<Response> productosPaginados = repoGenerico.findAll(pageable2);
        ListResponse listaProductos = (ListResponse)productosPaginados.getContent(); 

            return listaProductos;
        } catch (Exception e) {
            error.error(e);
        }
        return (ListResponse) new ArrayList<>();
    }

        @Override
    public Paginacion findAllNew(int pagina, int size) throws Exception{
        PginaDto<List<Response>> pginaDto = new PginaDto<>();
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<Response> dataPaginacion = this.repoGenerico.findAll(pageable);
        pginaDto.setPagina(pagina);
        pginaDto.setTotalPaginas(dataPaginacion.getTotalPages());
        pginaDto.setTotalRegistros((int) dataPaginacion.getTotalElements());
        pginaDto.setT(dataPaginacion.getContent() );

        return (Paginacion) pginaDto;
    }
}
