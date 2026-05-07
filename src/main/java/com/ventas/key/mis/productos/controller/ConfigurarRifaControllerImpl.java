package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.models.ConfigurarRifaResumenDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
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

    @GetMapping("/activas")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaResumenDto>>> getActivas() {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaResumenDto>>(sGenerico.buscarActivasResumen()));
    }

    @GetMapping("/activas/hoy")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaResumenDto>>> getActivasHoy() {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaResumenDto>>(sGenerico.buscarActivasHoyResumen()));
    }
}
