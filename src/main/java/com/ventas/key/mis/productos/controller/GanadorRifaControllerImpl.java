package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.GanadorRifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.SorteoEstadoDto;
import com.ventas.key.mis.productos.service.GanadorRifaServiceImpl;

@RestController
@RequestMapping("ganadorRifa")
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
    public ResponseEntity<ResponseGeneric<GanadorRifa>> sortear(
            @PathVariable int configurarRifaId,
            @RequestParam int vueltaActual,
            @RequestParam int totalVueltas) {
        try {
            GanadorRifa resultado = ganadorRifaService.sortear(configurarRifaId, vueltaActual, totalVueltas);
            return ResponseEntity.ok(new ResponseGeneric<>(resultado));
        } catch (Exception e) {
            ResponseGeneric<GanadorRifa> error = new ResponseGeneric<>((GanadorRifa) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/estado/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoEstadoDto>> obtenerEstado(@PathVariable int configurarRifaId) {
        try {
            SorteoEstadoDto estado = ganadorRifaService.obtenerEstado(configurarRifaId);
            return ResponseEntity.ok(new ResponseGeneric<>(estado));
        } catch (Exception e) {
            ResponseGeneric<SorteoEstadoDto> error = new ResponseGeneric<>((SorteoEstadoDto) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/reiniciar/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<String>> reiniciar(
            @PathVariable int configurarRifaId,
            @RequestParam(defaultValue = "false") boolean completo) {
        try {
            ganadorRifaService.reiniciar(configurarRifaId, completo);
            String msg = completo ? "Rifa reiniciada completamente (concursantes eliminados)" : "Rifa reiniciada (concursantes conservados)";
            return ResponseEntity.ok(new ResponseGeneric<>(msg));
        } catch (Exception e) {
            ResponseGeneric<String> error = new ResponseGeneric<>((String) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}