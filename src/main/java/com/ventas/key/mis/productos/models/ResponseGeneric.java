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
        if( data == null){
            this.data = null;
            this.code = 404;
            this.mensaje = mensaje;
        }
    }

        public ResponseGeneric(List<T> lista){
        if( lista == null || lista.isEmpty() ){
            this.lista = lista;
            this.code = 404;
            this.mensaje = "Ocurrio un error en la peticion";
        }
        this.lista = lista;
    }

        public ResponseGeneric(List<T> lista, String mensaje){
        if( lista == null || lista.isEmpty() ){
            this.lista = lista;
            this.code = 404;
            this.mensaje = mensaje;
        }
    }

}
