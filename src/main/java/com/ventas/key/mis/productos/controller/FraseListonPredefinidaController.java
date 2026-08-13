package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.FraseListonPredefinidaServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/frases-liston")
public class FraseListonPredefinidaController extends AbstractController<
        FraseListonPredefinida,
        Optional<FraseListonPredefinida>,
        List<FraseListonPredefinida>,
        Integer,
        PginaDto<List<FraseListonPredefinida>>,
        FraseListonPredefinidaServiceImpl> {

    public FraseListonPredefinidaController(FraseListonPredefinidaServiceImpl sGenerico) {
        super(sGenerico);
    }
}
