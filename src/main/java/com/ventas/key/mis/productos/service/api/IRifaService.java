package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;
import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface IRifaService extends ICrud<
                                            Rifa,
                                            List<Rifa>, 
                                            Optional<Rifa>, 
                                            Integer,
                                            PginaDto<List<Rifa>>>{

}
