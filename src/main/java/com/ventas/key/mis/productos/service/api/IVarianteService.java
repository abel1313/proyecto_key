package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

import java.util.List;
import java.util.Optional;

public interface IVarianteService extends ICrud<
                                            Variantes,
                                            List<Variantes>,
                                            Optional<Variantes>,
                                            Integer,
                                            PginaDto<List<Variantes>>> {

    PginaDto<List<Variantes>> buscarPorNombrePaginado(String nombre, int pagina, int size);
    PginaDto<List<Variantes>> buscarPorCodigoBarrasPaginado(String codigoBarras, int pagina, int size);

}
