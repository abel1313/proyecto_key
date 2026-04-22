package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.RifaServiceImpl;
import com.ventas.key.mis.productos.service.api.IRifaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("rifa")
public class RifaControllerImpl extends AbstractController<
        Rifa,
        Optional<Rifa>,
        List<Rifa>,
        Integer,
        PginaDto<List<Rifa>>,
        RifaServiceImpl> {

    private final IRifaService iRifaService;

    public RifaControllerImpl(RifaServiceImpl sGenerico) {
        super(sGenerico);
        this.iRifaService = sGenerico;
    }

    @PostMapping("/registrar")
    public ResponseEntity<ResponseGeneric<Rifa>> registrar(
            @Validated @RequestBody Rifa rifa,
            BindingResult result,
            @RequestParam(defaultValue = "false") boolean forzar) {
        if (result.hasErrors()) {
            String errores = result.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(new ResponseGeneric<>(null, errores));
        }
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(iRifaService.registrar(rifa, forzar)));
        } catch (Exception e) {
            log.error("Error al registrar concursante: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/listConcursantes/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<List<Rifa>>> getConcursantes(@PathVariable Integer configurarRifaId) {
        List<Rifa> concursantes = iRifaService.buscarPorConfiguracion(configurarRifaId);
        return ResponseEntity.ok(new ResponseGeneric<List<Rifa>>(concursantes));
    }

    @GetMapping("/getRifasPorHora")
    public ResponseEntity<List<Rifa>> getRifaPorHora(
            @RequestParam String inicio,
            @RequestParam String fin,
            @RequestParam String palabraRifa) throws Exception {
        return ResponseEntity.status(HttpStatus.OK)
                .body(iRifaService.buscarPorRangoDeHora(inicio, fin, palabraRifa));
    }

    @MessageMapping("/actualizar")
    @SendTo("/topic/ruleta")
    public String enviarActualizacion(String mensaje) {
        return mensaje;
    }
}
