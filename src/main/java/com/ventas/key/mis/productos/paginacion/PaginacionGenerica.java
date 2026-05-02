package com.ventas.key.mis.productos.paginacion;

import com.ventas.key.mis.productos.models.PginaDto;
import org.springframework.data.domain.Page;

import java.util.List;

public class PaginacionGenerica <T>{

    public PginaDto<List<T>> setPagina(Page<T> page, int pagina) {
        PginaDto<List<T>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }
}
