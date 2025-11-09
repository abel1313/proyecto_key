package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
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
    private static final int CODIGO_UNICO_SQL = 1062;
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
        return this.repoGenerico.save(req);
        } catch (Exception e) {
            error.error(e);
            throw typeError(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListResponse findAll(int pagina, int size) {
        try {
        
        Pageable pageable2 = PageRequest.of(pagina, size);

        Page<Response> productosPaginados = repoGenerico.findAll(pageable2);
        return (ListResponse)productosPaginados.getContent();
        } catch (Exception e) {
            error.error(e);
        }
        return (ListResponse) new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Paginacion findAllNew(int pagina, int size){
        PginaDto<List<Response>> pginaDto = new PginaDto<>();
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<Response> dataPaginacion = this.repoGenerico.findAll(pageable);
        pginaDto.setPagina(pagina);
        pginaDto.setTotalPaginas(dataPaginacion.getTotalPages());
        pginaDto.setTotalRegistros((int) dataPaginacion.getTotalElements());
        pginaDto.setT(dataPaginacion.getContent() );
        return (Paginacion) pginaDto;
    }

    @Override
    public Response update(TiopoDato tipoDato, Response req) throws Exception {
        ResponseOptional responseOptional;
        Response response = null;
        try{
            responseOptional = findById(tipoDato);
            if(responseOptional.isPresent()){
                response = save(req);
            }
        }catch(Exception e){
            error.error(e);
            throw new Exception(e.getMessage());
        }
        return response;
    }

    private GenericException typeError(Exception ex){
        Throwable causa = ex.getCause();
        if (causa instanceof ConstraintViolationException sqlEx) {
            int codigoSql = sqlEx.getErrorCode(); // ← aquí está el código del motor
            String estadoSql = sqlEx.getSQLState(); // ← también puedes usar esto

            throw new GenericException(codigoSql,"El codigo postal ya existe, ingrese uno diferente");
        }

        throw new GenericException(500, ex.getMessage());
    }
}
