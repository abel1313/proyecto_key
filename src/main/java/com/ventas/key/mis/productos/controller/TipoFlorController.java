package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.TipoFlorServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/tipos-flor")
public class TipoFlorController extends AbstractController<
        TipoFlor,
        Optional<TipoFlor>,
        List<TipoFlor>,
        Integer,
        PginaDto<List<TipoFlor>>,
        TipoFlorServiceImpl> {

    public TipoFlorController(TipoFlorServiceImpl sGenerico) {
        super(sGenerico);
    }
}
