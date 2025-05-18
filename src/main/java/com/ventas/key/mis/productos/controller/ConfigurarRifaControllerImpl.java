package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.ConfiguracionRifaServiceImpl;

@RestController
@RequestMapping("configurarRifa")
public class ConfigurarRifaControllerImpl extends AbstractController<
                                                            ConfigurarRifa,
                                                            Optional<ConfigurarRifa>,
                                                            List<ConfigurarRifa>,
                                                            Integer,
                                                            PginaDto<List<ConfigurarRifa>>,
                                                            ConfiguracionRifaServiceImpl
                                                            > {

    public ConfigurarRifaControllerImpl(ConfiguracionRifaServiceImpl sGenerico) {
        super(sGenerico);
    }

}
