package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.CantidadFlorValidaServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/cantidades-flor")
public class CantidadFlorValidaController extends AbstractController<
        CantidadFlorValida,
        Optional<CantidadFlorValida>,
        List<CantidadFlorValida>,
        Integer,
        PginaDto<List<CantidadFlorValida>>,
        CantidadFlorValidaServiceImpl> {

    public CantidadFlorValidaController(CantidadFlorValidaServiceImpl sGenerico) {
        super(sGenerico);
    }
}
