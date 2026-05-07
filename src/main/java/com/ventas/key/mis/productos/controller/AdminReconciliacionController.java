package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ReconciliacionResultadoDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ReconciliacionImagenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reconciliacion/imagenes")
@RequiredArgsConstructor
@Slf4j
public class AdminReconciliacionController {

    private final ReconciliacionImagenService reconciliacionImagenService;

    /**
     * Dispara la reconciliacion en segundo plano y responde de inmediato.
     * El front debe hacer polling a GET /resultado para ver cuando termino.
     */
    @PostMapping
    public ResponseEntity<ResponseGeneric<String>> reconciliar(
            @RequestParam(required = false) Integer productoId) {

        if (reconciliacionImagenService.isEnProceso()) {
            return ResponseEntity.ok(new ResponseGeneric<>("Ya hay una reconciliacion en proceso. Consulta GET /resultado para ver el avance."));
        }

        log.info("Reconciliacion manual iniciada por admin. productoId={}", productoId);
        if (productoId != null) {
            reconciliacionImagenService.reconciliarProducto(productoId);
        } else {
            reconciliacionImagenService.reconciliarTodos();
        }

        return ResponseEntity.ok(new ResponseGeneric<>("Reconciliacion iniciada. Consulta GET /resultado para ver cuando termina."));
    }

    /**
     * Elimina de BD (producto_imagen_copy, variante_imagen, imagenes_copy) todos los registros
     * cuyo archivo fisico no exista en disco. Corre en segundo plano.
     * Usa GET /resultado para ver cuando termino.
     */
    @PostMapping("/limpiar-bd")
    public ResponseEntity<ResponseGeneric<String>> limpiarBd() {
        if (reconciliacionImagenService.isEnProceso()) {
            return ResponseEntity.ok(new ResponseGeneric<>("Ya hay un proceso en curso. Consulta GET /resultado."));
        }
        log.info("Limpieza de BD iniciada por admin");
        reconciliacionImagenService.limpiarBdHuerfanos();
        return ResponseEntity.ok(new ResponseGeneric<>("Limpieza de BD iniciada. Consulta GET /resultado para ver cuando termina."));
    }

    /**
     * Devuelve el resultado de la ultima ejecucion.
     * enProceso=true significa que aun esta corriendo.
     * enProceso=false significa que ya termino.
     */
    @GetMapping("/resultado")
    public ResponseEntity<ResponseGeneric<ReconciliacionResultadoDto>> ultimoResultado() {
        return ResponseEntity.ok(new ResponseGeneric<>(reconciliacionImagenService.getUltimoResultado()));
    }
}