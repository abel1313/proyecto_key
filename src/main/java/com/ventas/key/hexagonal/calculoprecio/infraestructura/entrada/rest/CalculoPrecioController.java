package com.ventas.key.hexagonal.calculoprecio.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.CalculoPrecioInvalidoException;
import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.ProductoSinCalculoException;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.entrada.CalculoPrecioCasoUso;
import com.ventas.key.hexagonal.calculoprecio.infraestructura.dto.CalculoPrecioResponse;
import com.ventas.key.hexagonal.calculoprecio.infraestructura.dto.ComisionTerminalResponse;
import com.ventas.key.hexagonal.calculoprecio.infraestructura.dto.GuardarCalculoPrecioRequest;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] Cálculos de precio de un producto. Solo ROLE_ADMIN (SecurityConfig: /v1/calculos-precio/**).
 * Contrato en CAMBIOS_FRONT.md.
 */
@RestController
@RequestMapping("/v1/calculos-precio")
@RequiredArgsConstructor
public class CalculoPrecioController {

    private final CalculoPrecioCasoUso casoUso;

    @PostMapping
    public ResponseEntity<ResponseGeneric<CalculoPrecioResponse>> guardar(@RequestBody GuardarCalculoPrecioRequest r) {
        if (r == null || r.productoId() == null) throw new CalculoPrecioInvalidoException("Falta el producto del cálculo.");
        CalculoPrecio c = new CalculoPrecio(null, r.productoId(), r.costoCompra(), r.transporte(), r.otros(), r.comisionPct(),
                r.comisionDescripcion(), r.margenPct(), r.precioCalculado(), r.precioCobrado(), Boolean.TRUE.equals(r.actualizoPrecioCompra()),
                r.nota(), null, null);
        String usuario = AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseGeneric<>(CalculoPrecioResponse.de(casoUso.guardar(c, usuario))));
    }

    /** 204 si el producto nunca tuvo un cálculo. */
    @GetMapping("/producto/{productoId}/ultimo")
    public ResponseEntity<ResponseGeneric<CalculoPrecioResponse>> ultimo(@PathVariable int productoId) {
        return casoUso.ultimo(productoId)
                .map(c -> ResponseEntity.ok(new ResponseGeneric<>(CalculoPrecioResponse.de(c))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<ResponseGeneric<CalculoPrecioResponse>> historial(@PathVariable int productoId,
                                                                            @RequestParam(defaultValue = "10") int max) {
        List<CalculoPrecioResponse> lista = casoUso.historial(productoId, max).stream().map(CalculoPrecioResponse::de).toList();
        return ResponseEntity.ok(new ResponseGeneric<>(lista, "Historial de cálculos de precio"));
    }

    /** Las comisiones de terminal que ya están dadas de alta (forma de pago + plan, tarifa e IVA en %): la calculadora deja elegir una. */
    @GetMapping("/comisiones")
    public ResponseEntity<ResponseGeneric<ComisionTerminalResponse>> comisiones() {
        List<ComisionTerminalResponse> lista = casoUso.comisiones().stream().map(ComisionTerminalResponse::de).toList();
        return ResponseEntity.ok(new ResponseGeneric<>(lista, "Comisiones de terminal"));
    }

    @ExceptionHandler(CalculoPrecioInvalidoException.class)
    public ResponseEntity<ResponseGeneric<Object>> invalido(CalculoPrecioInvalidoException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ProductoSinCalculoException.class)
    public ResponseEntity<ResponseGeneric<Object>> sinProducto(ProductoSinCalculoException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    private static ResponseEntity<ResponseGeneric<Object>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<Object> r = new ResponseGeneric<>();
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
