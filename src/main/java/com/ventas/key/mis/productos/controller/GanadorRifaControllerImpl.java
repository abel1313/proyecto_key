package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.GanadorRifaServiceImpl;

@RestController
@RequestMapping("gfanadorRifa")
public class GanadorRifaControllerImpl extends AbstractController<
                                                            GanadorRifa,
                                                            Optional<GanadorRifa>,
                                                            List<GanadorRifa>,
                                                            Integer,
                                                            PginaDto<List<GanadorRifa>>,
                                                            GanadorRifaServiceImpl
                                                            > {

    public GanadorRifaControllerImpl(GanadorRifaServiceImpl sGenerico) {
        super(sGenerico);
    }
}
