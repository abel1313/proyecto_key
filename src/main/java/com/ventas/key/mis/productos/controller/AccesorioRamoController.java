package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.AccesorioRamoServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/accesorios-ramo")
public class AccesorioRamoController extends AbstractController<
        AccesorioRamo,
        Optional<AccesorioRamo>,
        List<AccesorioRamo>,
        Integer,
        PginaDto<List<AccesorioRamo>>,
        AccesorioRamoServiceImpl> {

    public AccesorioRamoController(AccesorioRamoServiceImpl sGenerico) {
        super(sGenerico);
    }
}
