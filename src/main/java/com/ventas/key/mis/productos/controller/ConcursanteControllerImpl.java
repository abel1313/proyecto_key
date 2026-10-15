package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.models.ClientePedidoDto;
import com.ventas.key.mis.productos.models.ConcursanteEditarRequest;
import com.ventas.key.mis.productos.models.CopiarConcursantesRequest;
import com.ventas.key.mis.productos.models.ImportarDePedidosRequest;
import com.ventas.key.mis.productos.models.ImportarDePedidosResponseDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ConcursanteServiceImpl;
import com.ventas.key.mis.productos.service.api.IConcursanteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/concursante")
public class ConcursanteControllerImpl extends AbstractController<
        Concursante,
        Optional<Concursante>,
        List<Concursante>,
        Integer,
        PginaDto<List<Concursante>>,
        ConcursanteServiceImpl> {

    private final IConcursanteService iConcursanteService;

    public ConcursanteControllerImpl(ConcursanteServiceImpl sGenerico) {
        super(sGenerico);
        this.iConcursanteService = sGenerico;
    }

    @PostMapping("/registrar")
    public ResponseEntity<ResponseGeneric<Concursante>> registrar(
            @Validated @RequestBody Concursante concursante,
            BindingResult result,
            @RequestParam(defaultValue = "false") boolean forzar) throws Exception {
        if (result.hasErrors()) {
            String errores = result.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(new ResponseGeneric<>(null, errores));
        }
        return ResponseEntity.ok(new ResponseGeneric<>(iConcursanteService.registrar(concursante, forzar)));
    }

    @GetMapping("/porRifa/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<List<Concursante>>> getConcursantes(@PathVariable Integer configurarRifaId) {
        return ResponseEntity.ok(new ResponseGeneric<List<Concursante>>(iConcursanteService.buscarPorConfiguracion(configurarRifaId)));
    }

    @GetMapping("/elegibles/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<List<Concursante>>> getElegibles(@PathVariable Integer configurarRifaId) {
        return ResponseEntity.ok(new ResponseGeneric<List<Concursante>>(iConcursanteService.buscarElegibles(configurarRifaId)));
    }

    @GetMapping("/clientesPorMes")
    public ResponseEntity<ResponseGeneric<List<ClientePedidoDto>>> clientesPorMes(
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(new ResponseGeneric<List<ClientePedidoDto>>(sGenerico.clientesPorMes(mes)));
    }

    @PostMapping("/importarDePedidos")
    public ResponseEntity<ResponseGeneric<ImportarDePedidosResponseDto>> importarDePedidos(
            @RequestBody ImportarDePedidosRequest req) throws Exception {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.importarDePedidos(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) throws Exception {
        sGenerico.eliminar(id);
        return ResponseEntity.ok(new ResponseGeneric<>("Concursante eliminado"));
    }

    @PostMapping("/copiarDeRifa")
    public ResponseEntity<ResponseGeneric<List<Concursante>>> copiarDeRifa(
            @RequestBody CopiarConcursantesRequest req) throws Exception {
        return ResponseEntity.ok(new ResponseGeneric<List<Concursante>>(sGenerico.copiarDeRifa(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<Concursante>> editar(
            @PathVariable Integer id,
            @RequestBody ConcursanteEditarRequest req) throws Exception {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.editar(id, req)));
    }
}