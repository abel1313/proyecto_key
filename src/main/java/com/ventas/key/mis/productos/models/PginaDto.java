package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PginaDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private int pagina;
    private int totalPaginas;
    private int totalRegistros;
    private T t;
}
