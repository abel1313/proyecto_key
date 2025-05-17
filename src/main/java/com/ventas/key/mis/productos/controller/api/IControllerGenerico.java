package com.ventas.key.mis.productos.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.ventas.key.mis.productos.models.ResponseGeneric;

public interface IControllerGenerico<
                                    ResponseG,
                                    OptionalResponse,
                                    ListResponse,
                                    TipoDato> {

    ResponseEntity<ResponseGeneric<ResponseG>> save(ResponseG requestG, BindingResult result);
    ResponseEntity<ResponseGeneric<ListResponse>> findAll(int page, int size);
    ResponseEntity<ResponseGeneric<OptionalResponse>> findBy(TipoDato tipoDato);
    ResponseEntity<ResponseGeneric<ResponseG>> delete(TipoDato requestG);

}
