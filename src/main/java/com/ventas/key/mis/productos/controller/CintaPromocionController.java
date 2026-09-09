package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.CintaPromocion;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CintaPromocionServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/cinta")
public class CintaPromocionController extends AbstractController<
        CintaPromocion,
        Optional<CintaPromocion>,
        List<CintaPromocion>,
        Integer,
        PginaDto<List<CintaPromocion>>,
        CintaPromocionServiceImpl> {

    public CintaPromocionController(CintaPromocionServiceImpl sGenerico) {
        super(sGenerico);
    }

    // Publico -- ver SecurityConfig, es el unico GET de este controlador sin auth.
    @GetMapping("/activos")
    public ResponseEntity<ResponseGeneric<List<CintaPromocion>>> activos() {
        return ResponseEntity.ok(new ResponseGeneric<List<CintaPromocion>>(sGenerico.obtenerActivos()));
    }
}
