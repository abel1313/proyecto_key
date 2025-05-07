package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PginaDto<T> {

    private int pagina;
    private int totalPaginas;
    private int totalRegistros;
    private T t;
}
