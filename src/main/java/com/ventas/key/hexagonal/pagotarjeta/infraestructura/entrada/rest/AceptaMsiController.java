package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfigurarAceptaMsiCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto.AceptaMsiRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * La casilla "Acepta meses sin intereses" de productos y artículos. Solo administrador.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter] Traduce HTTP al caso de uso y nada más.
 * Existe solo con {@code pagos.tarjeta.habilitado=true}, igual que el resto del módulo.
 */
@RestController
@RequestMapping("/v1/acepta-msi")
@ConditionalOnProperty(name = "pagos.tarjeta.habilitado", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class AceptaMsiController {

    private final ConfigurarAceptaMsiCasoUso casillas;

    /** {aceptaMsi} del producto. */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<Object> producto(@PathVariable Integer productoId) {
        return ejecutar(() -> Map.of("aceptaMsi", casillas.aceptaProducto(productoId)));
    }

    /** Body {aceptaMsi: true|false}; en un producto no se admite null. */
    @PutMapping("/producto/{productoId}")
    public ResponseEntity<Object> cambiarProducto(@PathVariable Integer productoId, @RequestBody AceptaMsiRequest request) {
        return ejecutar(() -> {
            if (request == null || request.aceptaMsi() == null) {
                throw PagoConTarjetaException.datosInvalidos("el producto debe aceptar o no aceptar meses (true o false)");
            }
            casillas.cambiarProducto(productoId, request.aceptaMsi());
            return Map.of("aceptaMsi", request.aceptaMsi());
        });
    }

    /** {propio, heredaDelProducto, delProducto, efectivo}: propio null = hereda. */
    @GetMapping("/articulo/{varianteId}")
    public ResponseEntity<Object> articulo(@PathVariable Integer varianteId) {
        return ejecutar(() -> detalle(casillas.articulo(varianteId)));
    }

    /** Body {aceptaMsi: true|false|null}: null = que el artículo herede la casilla de su producto. */
    @PutMapping("/articulo/{varianteId}")
    public ResponseEntity<Object> cambiarArticulo(@PathVariable Integer varianteId, @RequestBody AceptaMsiRequest request) {
        return ejecutar(() -> {
            casillas.cambiarArticulo(varianteId, request != null ? request.aceptaMsi() : null);
            return detalle(casillas.articulo(varianteId));
        });
    }

    /** Valor efectivo de varios artículos a la vez: {"12": true, "13": false}. Para pintar el chip MSI en las listas. */
    @GetMapping("/articulos")
    public ResponseEntity<Object> articulos(@RequestParam("ids") List<Integer> ids) {
        return ejecutar(() -> casillas.efectivos(ids));
    }

    private static Map<String, Object> detalle(ConfigMsiArticulo c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("propio", c.propio());
        m.put("heredaDelProducto", c.hereda());
        m.put("delProducto", c.delProducto());
        m.put("efectivo", c.efectivo());
        return m;
    }

    private ResponseEntity<Object> ejecutar(Supplier<Object> operacion) {
        try {
            if (!AuthenticationUtils.isAdminContext()) {
                return error(HttpStatus.NOT_FOUND, "No se encontró");
            }
            return ResponseEntity.ok(new ResponseGeneric<>(operacion.get()));
        } catch (PagoConTarjetaException.NoExiste e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (PagoConTarjetaException.CasillaMsiNoDisponible e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (PagoConTarjetaException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error en la casilla de meses sin intereses", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación");
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
        error.setCode(status.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(status).body(error);
    }
}
