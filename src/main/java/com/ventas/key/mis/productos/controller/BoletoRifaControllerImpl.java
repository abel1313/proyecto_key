package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.BoletoRifaRequest;
import com.ventas.key.mis.productos.models.PremioPublicoDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.SorteoPlataformasDto;
import com.ventas.key.mis.productos.models.SorteoPlataformasResultadoDto;
import com.ventas.key.mis.productos.service.BoletoRifaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/boletoRifa")
@RequiredArgsConstructor
@Slf4j
public class BoletoRifaControllerImpl {

    private final BoletoRifaServiceImpl service;

    @PostMapping("/registrar")
    public ResponseEntity<ResponseGeneric<BoletoRifa>> registrar(@RequestBody BoletoRifaRequest req) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.registrar(req)));
        } catch (Exception e) {
            log.error("Error al registrar boleto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/porConcursante/{concursanteId}")
    public ResponseEntity<ResponseGeneric<List<BoletoRifa>>> porConcursante(@PathVariable Integer concursanteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<BoletoRifa>>(service.listarPorConcursante(concursanteId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<BoletoRifa>> editar(
            @PathVariable Integer id, @RequestBody BoletoRifaRequest req) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.editar(id, req)));
        } catch (Exception e) {
            log.error("Error al editar boleto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Boleto eliminado"));
        } catch (Exception e) {
            log.error("Error al eliminar boleto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    // ── Sorteo de la rifa PLATAFORMAS (se sortea entre boletos) ────────

    @GetMapping("/estado/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoPlataformasDto>> estado(@PathVariable Integer configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.obtenerEstado(configurarRifaId)));
        } catch (Exception e) {
            log.error("Error al obtener estado de la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/sortear/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoPlataformasResultadoDto>> sortear(@PathVariable Integer configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.sortear(configurarRifaId)));
        } catch (Exception e) {
            log.error("Error al sortear la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/reiniciar/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<String>> reiniciar(@PathVariable Integer configurarRifaId) {
        try {
            service.reiniciar(configurarRifaId);
            return ResponseEntity.ok(new ResponseGeneric<>("Rifa reiniciada"));
        } catch (Exception e) {
            log.error("Error al reiniciar la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    // ── Página pública de la ruleta (sin sesión) ───────────────────────
    // Cualquiera con el link ve la ruleta y puede girar MIENTRAS la rifa sea de
    // prueba. La rifa real solo la mueve el admin desde los endpoints de arriba.
    // Lo que se devuelve aquí va recortado: sin URLs de evidencia ni datos de
    // contacto de los concursantes.

    @GetMapping("/publico/estado/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoPlataformasDto>> estadoPublico(@PathVariable Integer configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.obtenerEstado(configurarRifaId, true)));
        } catch (ExceptionDataNotFound e) {
            return noEncontrada(configurarRifaId, e);
        } catch (Exception e) {
            log.error("Error al obtener estado público de la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    /** Ficha del premio con todas sus fotos, para el detalle que abre el visitante. */
    @GetMapping("/publico/premio/{configurarRifaId}/{premioId}")
    public ResponseEntity<ResponseGeneric<PremioPublicoDto>> premioPublico(
            @PathVariable Integer configurarRifaId, @PathVariable Integer premioId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    service.detallePremioPublico(configurarRifaId, premioId)));
        } catch (ExceptionDataNotFound e) {
            return noEncontrada(configurarRifaId, e);
        } catch (Exception e) {
            log.error("Error al obtener el premio {} de la rifa {}: {}", premioId, configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/publico/sortear/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<SorteoPlataformasResultadoDto>> sortearPublico(@PathVariable Integer configurarRifaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.sortear(configurarRifaId, true)));
        } catch (ExceptionDataNotFound e) {
            return noEncontrada(configurarRifaId, e);
        } catch (Exception e) {
            log.error("Error al sortear (público) la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/publico/reiniciar/{configurarRifaId}")
    public ResponseEntity<ResponseGeneric<String>> reiniciarPublico(@PathVariable Integer configurarRifaId) {
        try {
            service.reiniciar(configurarRifaId, true);
            return ResponseEntity.ok(new ResponseGeneric<>("Rifa reiniciada"));
        } catch (ExceptionDataNotFound e) {
            return noEncontrada(configurarRifaId, e);
        } catch (Exception e) {
            log.error("Error al reiniciar (público) la rifa {}: {}", configurarRifaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    // El link público lleva el id en la URL y se puede tantear cambiando el número, así
    // que la rifa que no está publicada responde 404 igual que una que no existe: el
    // visitante no puede distinguir una de otra ni ir descubriendo qué rifas hay.
    private <T> ResponseEntity<ResponseGeneric<T>> noEncontrada(Integer rifaId, ExceptionDataNotFound e) {
        log.warn("Acceso público a rifa no publicada o inexistente {}: {}", rifaId, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseGeneric<>(null, "Rifa no encontrada"));
    }
}
