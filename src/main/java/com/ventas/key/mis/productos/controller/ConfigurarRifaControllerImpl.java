package com.ventas.key.mis.productos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.models.ConfigurarRifaPatchDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaResumenDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ConfiguracionRifaServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/configurarRifa")
public class ConfigurarRifaControllerImpl extends AbstractController<
                                                            ConfigurarRifa,
                                                            Optional<ConfigurarRifa>,
                                                            List<ConfigurarRifa>,
                                                            Integer,
                                                            PginaDto<List<ConfigurarRifa>>,
                                                            ConfiguracionRifaServiceImpl
                                                            > {

    public ConfigurarRifaControllerImpl(ConfiguracionRifaServiceImpl sGenerico) {
        super(sGenerico);
    }

    @GetMapping("/activas")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaResumenDto>>> getActivas() {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaResumenDto>>(sGenerico.buscarActivasResumen()));
    }

    @GetMapping("/activas/hoy")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaResumenDto>>> getActivasHoy() {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaResumenDto>>(sGenerico.buscarActivasHoyResumen()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaResumenDto>>> buscar(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) ConfigurarRifa.TipoRifa tipo,
            @RequestParam(required = false) String mesReferencia) {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaResumenDto>>(sGenerico.buscar(desde, hasta, tipo, mesReferencia)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<ConfigurarRifa>> actualizarConfiguracion(
            @PathVariable Integer id,
            @RequestBody ConfigurarRifaPatchDto body) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.actualizarConfiguracion(id, body)));
        } catch (Exception e) {
            log.error("Error al actualizar configuración de rifa {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}/esPrueba")
    public ResponseEntity<ResponseGeneric<ConfigurarRifa>> toggleEsPrueba(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        try {
            boolean esPrueba = Boolean.TRUE.equals(body.get("esPrueba"));
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.toggleEsPrueba(id, esPrueba)));
        } catch (Exception e) {
            log.error("Error al cambiar esPrueba de la rifa {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
