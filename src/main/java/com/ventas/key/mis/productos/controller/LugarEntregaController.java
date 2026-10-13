package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.models.AnilloRequest;
import com.ventas.key.mis.productos.models.AnilloResponse;
import com.ventas.key.mis.productos.models.CalcularCostoEnvioRequest;
import com.ventas.key.mis.productos.models.CalcularCostoEnvioResponse;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.LugarEntregaAnilloServiceImpl;
import com.ventas.key.mis.productos.service.LugarEntregaServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/lugares-entrega")
public class LugarEntregaController extends AbstractController<
        LugarEntrega,
        Optional<LugarEntrega>,
        List<LugarEntrega>,
        Integer,
        PginaDto<List<LugarEntrega>>,
        LugarEntregaServiceImpl> {

    private final LugarEntregaAnilloServiceImpl anilloService;

    public LugarEntregaController(LugarEntregaServiceImpl sGenerico, LugarEntregaAnilloServiceImpl anilloService) {
        super(sGenerico);
        this.anilloService = anilloService;
    }

    // ── Anillos (rangos de distancia con precio propio) — ver DISENO_ZONAS_POR_ANILLO.md ──────

    @GetMapping("/{lugarEntregaId}/anillos")
    public ResponseEntity<ResponseGeneric<List<AnilloResponse>>> listarAnillos(@PathVariable Integer lugarEntregaId) {
        return ResponseEntity.ok(new ResponseGeneric<List<AnilloResponse>>(anilloService.listar(lugarEntregaId)));
    }

    @PostMapping("/{lugarEntregaId}/anillos")
    public ResponseEntity<ResponseGeneric<AnilloResponse>> crearAnillo(@PathVariable Integer lugarEntregaId,
                                                                        @RequestBody AnilloRequest req) {
        return ResponseEntity.ok(new ResponseGeneric<>(anilloService.crear(lugarEntregaId, req)));
    }

    @PutMapping("/anillos/{anilloId}")
    public ResponseEntity<ResponseGeneric<AnilloResponse>> editarAnillo(@PathVariable Integer anilloId,
                                                                         @RequestBody AnilloRequest req) {
        return ResponseEntity.ok(new ResponseGeneric<>(anilloService.editar(anilloId, req)));
    }

    @DeleteMapping("/anillos/{anilloId}")
    public ResponseEntity<ResponseGeneric<Void>> eliminarAnillo(@PathVariable Integer anilloId) {
        anilloService.eliminar(anilloId);
        return ResponseEntity.ok(new ResponseGeneric<>((Void) null));
    }

    // Publico (ver SecurityConfig) -- lo llama el checkout antes de dejar avanzar/confirmar, y
    // "Info de entrega" para mostrar el diferencial antes de aplicar un cambio de zona/punto.
    @PostMapping("/{lugarEntregaId}/calcular-costo")
    public ResponseEntity<ResponseGeneric<CalcularCostoEnvioResponse>> calcularCosto(
            @PathVariable Integer lugarEntregaId, @RequestBody CalcularCostoEnvioRequest req) {
        CalcularCostoEnvioResponse resp =
                anilloService.calcularCosto(lugarEntregaId, req.getLatitud(), req.getLongitud());
        return ResponseEntity.ok(new ResponseGeneric<>(resp));
    }
}
