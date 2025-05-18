package com.ventas.key.mis.productos.service.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

public interface IRifaService extends ICrud<
                                            Rifa,
                                            List<Rifa>, 
                                            Optional<Rifa>, 
                                            Integer,
                                            PginaDto<List<Rifa>>>{



    List<Rifa> buscarPorRangoDeHora(String inicio, String fin, String palabraRifa)throws Exception;

}
