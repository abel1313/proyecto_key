package com.ventas.key.mis.productos.service.api;

import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface IConfiguracionRifaService extends ICrud<
                                                ConfigurarRifa,
                                                List<ConfigurarRifa>, 
                                                Optional<ConfigurarRifa>, 
                                                Integer,
                                                PginaDto<List<ConfigurarRifa>>>{

}
