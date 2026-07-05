package com.ventas.key.mis.productos.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseGeneric<T> {

    private String mensaje;
    private int code;
    private T data;
    private List<T> lista;


    public ResponseGeneric(T data){
        this.data = data;
        if( data == null){
            this.code = 404;
            this.mensaje = "Ocurrio un error en la peticion";
        }else{
            this.code = 200;
            this.mensaje = "La peticion fue exitosa";
        }
    }

        public ResponseGeneric(T data, String mensaje){
        this.data = data;
        this.mensaje = mensaje;
        this.code = (data == null) ? 404 : 200;
    }

        public ResponseGeneric(List<T> lista){
        this.lista = lista;
        if( lista == null || lista.isEmpty() ){
            this.code = 404;
            this.mensaje = "Ocurrio un error en la peticion";
        }else{
            this.code = 200;
            this.mensaje = "La peticion fue exitosa";
        }
    }

        public ResponseGeneric(List<T> lista, String mensaje){
        this.lista = lista;
        this.mensaje = mensaje;
        this.code = (lista == null || lista.isEmpty()) ? 404 : 200;
    }

}
