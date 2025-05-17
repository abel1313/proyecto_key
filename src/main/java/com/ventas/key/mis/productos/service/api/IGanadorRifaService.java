package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface IGanadorRifaService extends ICrud<
                                                GanadorRifa,
                                                List<GanadorRifa>, 
                                                Optional<GanadorRifa>, 
                                                Integer,
                                                PginaDto<List<GanadorRifa>>>{

}
