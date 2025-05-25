package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface ICodigoBarrasService extends ICrud<
                                                CodigoBarra,
                                                List<CodigoBarra>, 
                                                Optional<CodigoBarra>, 
                                                Integer,
                                                PginaDto<List<CodigoBarra>>>{

    public Optional<CodigoBarra> findByCodigoBarra(String codigoBarra);

}
