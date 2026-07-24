package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.LugarEntregaServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/lugares-entrega")
public class LugarEntregaController extends AbstractController<
        LugarEntrega,
        Optional<LugarEntrega>,
        List<LugarEntrega>,
        Integer,
        PginaDto<List<LugarEntrega>>,
        LugarEntregaServiceImpl> {

    public LugarEntregaController(LugarEntregaServiceImpl sGenerico) {
        super(sGenerico);
    }
}
