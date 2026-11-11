package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.LigarVentasAnterioresCasoUso;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.LigarVentasRequest;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.LigaManualResponse;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.LineaSinViajeResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * [Hexagonal: Driving Adapter] Ligar a mano ventas anteriores a un viaje de compra (solo administrador; regla {@code /v1/fletes/**} en {@code SecurityConfig}).
 * Sin lógica: traduce HTTP al caso de uso; un rechazo de las reglas sale como {@code 400} con el mensaje en palabras del dueño.
 */
@RestController
@RequestMapping("/v1/fletes")
@RequiredArgsConstructor
@Slf4j
public class LigasManualesController {

    private final LigarVentasAnterioresCasoUso ligar;

    /** Las líneas de venta del producto con piezas aún sin ligar a un viaje (hasta 500, de la más vieja a la más nueva). La lista va en {@code lista}. */
    @GetMapping("/ligas/sin-viaje")
    public ResponseEntity<ResponseGeneric<LineaSinViajeResponse>> sinViaje(
            @RequestParam int productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ligar.lineasSinViaje(productoId, desde, hasta).stream().map(LineaSinViajeResponse::de).toList()));
        } catch (ViajeInvalidoException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al buscar ventas sin viaje", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron buscar las ventas.");
        }
    }

    /** Liga las piezas indicadas de varias líneas de venta del producto al viaje (guarda el flete por pieza de ese viaje). {@code data} = piezas ligadas. */
    @Transactional
    @PostMapping("/{viajeId}/ligar")
    public ResponseEntity<ResponseGeneric<Integer>> ligar(@PathVariable int viajeId, @RequestBody LigarVentasRequest request) {
        try {
            if (request == null || request.productoId() == null) {
                throw new ViajeInvalidoException("Falta elegir el producto.");
            }
            int piezas = ligar.ligar(viajeId, request.productoId(), request.aDominio());
            ResponseGeneric<Integer> r = new ResponseGeneric<>(piezas);
            r.setMensaje("Se ligaron " + piezas + " pieza(s) al viaje.");
            return ResponseEntity.ok(r);
        } catch (ViajeInvalidoException e) {
            return rechazo(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al ligar ventas a un viaje", e);
            throw e;   // la transacción se deshace y no queda nada a medias
        }
    }

    /** Las ligas hechas a mano de un viaje (para poder deshacerlas). La lista va en {@code lista}. */
    @GetMapping("/{viajeId}/ligas")
    public ResponseEntity<ResponseGeneric<LigaManualResponse>> ligasDelViaje(@PathVariable int viajeId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ligar.ligasManualesDelViaje(viajeId).stream().map(LigaManualResponse::de).toList()));
        } catch (RuntimeException e) {
            log.error("Error al listar las ligas manuales del viaje {}", viajeId, e);
            ResponseGeneric<LigaManualResponse> r = new ResponseGeneric<>((LigaManualResponse) null);
            r.setCode(500);
            r.setMensaje("No se pudieron cargar las ligas del viaje.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(r);
        }
    }

    /** Deshace una liga hecha a mano (las automáticas no se tocan). */
    @DeleteMapping("/ligas/{ligaId}")
    public ResponseEntity<ResponseGeneric<Integer>> quitar(@PathVariable int ligaId) {
        try {
            ligar.quitarLiga(ligaId);
            ResponseGeneric<Integer> r = new ResponseGeneric<>(ligaId);
            r.setMensaje("Liga deshecha.");
            return ResponseEntity.ok(r);
        } catch (ViajeInvalidoException e) {
            return rechazo(e.getMessage());
        }
    }

    private static ResponseEntity<ResponseGeneric<Integer>> rechazo(String mensaje) {
        ResponseGeneric<Integer> r = new ResponseGeneric<>((Integer) null);
        r.setCode(400);
        r.setMensaje(mensaje);
        return ResponseEntity.badRequest().body(r);
    }

    private static ResponseEntity<ResponseGeneric<LineaSinViajeResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<LineaSinViajeResponse> r = new ResponseGeneric<>((LineaSinViajeResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
