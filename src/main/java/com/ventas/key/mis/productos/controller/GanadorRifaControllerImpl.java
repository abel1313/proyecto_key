package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.SorteoEstadoDto;
import com.ventas.key.mis.productos.models.SorteoResultadoDto;
import com.ventas.key.mis.productos.service.GanadorRifaServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v1/ganadorRifa")
public class GanadorRifaControllerImpl extends AbstractController<
        GanadorRifa,
        Optional<GanadorRifa>,
        List<GanadorRifa>,
        Integer,
        PginaDto<List<GanadorRifa>>,
        GanadorRifaServiceImpl> {

    private final GanadorRifaServiceImpl ganadorRifaService;

    public GanadorRifaControllerImpl(GanadorRifaServiceImpl sGenerico) {
        super(sGenerico);
        this.ganadorRifaService = sGenerico;
    }

    @PostMapping("/sortear/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoResultadoDto>> sortear(@PathVariable int configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ganadorRifaService.sortear(configurarRifaId)));
        } catch (Exception e) {
            log.error("Error en sorteo rifa={}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/continuarVariante/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoEstadoDto>> continuarVariante(
            @PathVariable int configurarRifaId,
            @RequestParam String modo) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    ganadorRifaService.continuarVariante(configurarRifaId, modo)));
        } catch (Exception e) {
            log.error("Error al continuar variante rifa={}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/estado/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoEstadoDto>> obtenerEstado(
            @PathVariable int configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ganadorRifaService.obtenerEstado(configurarRifaId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/reiniciar/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<String>> reiniciar(
            @PathVariable int configurarRifaId,
            @RequestParam(defaultValue = "false") boolean completo) {
        try {
            ganadorRifaService.reiniciar(configurarRifaId, completo);
            String msg = completo
                    ? "Rifa reiniciada completamente (concursantes eliminados)"
                    : "Rifa reiniciada (concursantes conservados)";
            return ResponseEntity.ok(new ResponseGeneric<>(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}