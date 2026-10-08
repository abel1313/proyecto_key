package com.ventas.key.hexagonal.stock.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.stock.dominio.excepcion.AjusteStockInvalidoException;
import com.ventas.key.hexagonal.stock.dominio.excepcion.ProductoSinStockConocidoException;
import com.ventas.key.hexagonal.stock.dominio.puerto.entrada.AjustarStockModeloCasoUso;
import com.ventas.key.hexagonal.stock.dominio.puerto.entrada.ConsultarDisponibilidadCasoUso;
import com.ventas.key.hexagonal.stock.infraestructura.dto.AjusteStockRequest;
import com.ventas.key.hexagonal.stock.infraestructura.dto.DisponibilidadStockResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>El {@code /v1/} va en el @RequestMapping de la clase, nunca en los metodos (ver CLAUDE.md).
 *
 * <p>Depende del puerto de entrada, no del servicio concreto: la flecha apunta hacia adentro.
 */
@RestController
@RequestMapping("/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final ConsultarDisponibilidadCasoUso casoUso;
    private final AjustarStockModeloCasoUso ajustarStock;

    /**
     * Cuanto stock queda libre para armar variantes de este producto.
     *
     * <p>La pantalla de alta/edicion lo pide antes de que el admin escriba un numero, para que
     * sepa cuanto puede pedir en vez de enterarse por un error al guardar.
     */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<DisponibilidadStockResponse> disponibilidad(@PathVariable Integer productoId) {
        return ResponseEntity.ok(DisponibilidadStockResponse.de(casoUso.de(productoId)));
    }

    /**
     * Solo ADMIN -- productos cuyas variantes piden mas stock del que el producto declara.
     *
     * <p>Es el reporte a mirar antes de migrar el modelo de stock: dice cuales estan
     * descuadrados y por cuanto, que es la decision que no se puede automatizar.
     */
    /**
     * Agregar (+) o quitar (-) stock al modelo desde Agregar articulo (2026-10-08). Lo agregado queda
     * libre; responde como quedo para que la pantalla diga cuantos articulos caben.
     */
    @PutMapping("/producto/{productoId}/ajuste")
    public ResponseEntity<DisponibilidadStockResponse> ajustar(@PathVariable Integer productoId,
                                                               @RequestBody AjusteStockRequest request) {
        int ajuste = request != null && request.ajuste() != null ? request.ajuste() : 0;
        return ResponseEntity.ok(DisponibilidadStockResponse.de(ajustarStock.ajustar(productoId, ajuste)));
    }

    @ExceptionHandler(AjusteStockInvalidoException.class)
    public ResponseEntity<ResponseGeneric<Object>> ajusteInvalido(AjusteStockInvalidoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
    }

    @ExceptionHandler(ProductoSinStockConocidoException.class)
    public ResponseEntity<ResponseGeneric<Object>> noExiste(ProductoSinStockConocidoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseGeneric<>(null, e.getMessage()));
    }

    @GetMapping("/admin/descuadrados")
    public ResponseEntity<List<DisponibilidadStockResponse>> descuadrados() {
        return ResponseEntity.ok(casoUso.descuadrados().stream()
                .map(DisponibilidadStockResponse::de)
                .toList());
    }
}
