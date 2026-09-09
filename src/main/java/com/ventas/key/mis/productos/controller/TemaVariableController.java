package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.TemaVariable;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.TemaVariableServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

// Catálogo dinámico de variables de personalización visual -- ver TemaVariable.java. "Dar de
// alta" una variable nueva se hace con el POST /save heredado, igual que Menu/Submenu.
@RestController
@RequestMapping("/v1/tema-variable")
public class TemaVariableController extends AbstractController<
        TemaVariable,
        Optional<TemaVariable>,
        List<TemaVariable>,
        Integer,
        PginaDto<List<TemaVariable>>,
        TemaVariableServiceImpl> {

    public TemaVariableController(TemaVariableServiceImpl sGenerico) {
        super(sGenerico);
    }

    /** Público -- hasta un visitante anónimo viendo la tienda necesita aplicar el tema activo. */
    @GetMapping("/activo")
    public ResponseEntity<ResponseGeneric<List<TemaVariable>>> activo() {
        return ResponseEntity.ok(new ResponseGeneric<List<TemaVariable>>(sGenerico.activas()));
    }
}
