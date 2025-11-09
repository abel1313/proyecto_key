package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.controller.api.IControllerGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CrudAbstractServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractController<
                                         Response,
                                         OptionalResponse extends Optional<Response>,
                                         ListResponse extends List<Response>,
                                         TipoDato,
                                         Paginacion extends PginaDto<List<Response>>,
                                         ServiceG extends CrudAbstractServiceImpl<
                                         Response,
                                         ListResponse,
                                         OptionalResponse,
                                         TipoDato,
                                         Paginacion 
                                         >>
                                         implements
                                         IControllerGenerico<
                                         Response,
                                         OptionalResponse,
                                         ListResponse,
                                         TipoDato
                                         > {


    private final ServiceG sGenerico;
    public AbstractController(
        final ServiceG sGenerico
    ){
        this.sGenerico = sGenerico;
    }
    
    @DeleteMapping("/delete")
    @Override
    public ResponseEntity<ResponseGeneric<Response>> delete(@RequestBody TipoDato requestG) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(this.sGenerico.delete(requestG)));
        } catch (Exception e) {
            return null;
        }
    }
    @GetMapping("/getAll")
    @Override
    public ResponseEntity<ResponseGeneric<ListResponse>> findAll(@RequestParam int page, @RequestParam int size) {
        try {
            ListResponse listResponse  = this.sGenerico.findAll(page, size);
            ResponseGeneric<ListResponse> respo = new ResponseGeneric<ListResponse>(listResponse);
            return ResponseEntity.status(HttpStatus.OK).body(respo);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/getOne/{tipoDato}")
    @Override
    public ResponseEntity<ResponseGeneric<OptionalResponse>> findBy(@PathVariable TipoDato tipoDato) {
        try {
            OptionalResponse listResponse  = this.sGenerico.findById(tipoDato);
            ResponseGeneric<OptionalResponse> respo = new ResponseGeneric<>(listResponse);
            return ResponseEntity.status(HttpStatus.OK).body(respo);
        } catch (Exception e) {
            return null;
        }
        
    }
    @PostMapping("/save")
    @Override
    public ResponseEntity<ResponseGeneric<Response>> save(@Validated @RequestBody Response requestG, BindingResult result) {
        try {
            if( result.hasErrors()){
                String errores = result.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
                ResponseGeneric<Response> erroResponse = new ResponseGeneric<>((Response) null);
                erroResponse.setMensaje(errores);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResponse);
            }
            Response response = this.sGenerico.save(requestG);
            log.info("ingo data {} ",response);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @PutMapping("/update/{tipoDato}")
    public ResponseEntity<ResponseGeneric<Response>> update(@PathVariable TipoDato tipoDato, Response requestG, BindingResult result) throws Exception {
        try {
            if(result.hasErrors()){
                ResponseGeneric<Response> erroResponse = new ResponseGeneric<>((Response) null);
                erroResponse.setMensaje(getErroresGeneric(result));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResponse);
            }
            Response response = this.sGenerico.save(requestG);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private String  getErroresGeneric(BindingResult result){
        return result.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
    }
}
