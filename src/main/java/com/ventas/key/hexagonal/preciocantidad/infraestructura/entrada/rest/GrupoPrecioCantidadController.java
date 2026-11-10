package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.AdministrarGruposCasoUso;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto.GrupoPrecioCantidadRequest;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto.GrupoPrecioCantidadResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>{@code /v1/grupos-precio-cantidad}: los grupos de precio por cantidad. Solo administrador (regla propia en {@code SecurityConfig}). Sin lógica:
 * traduce HTTP al caso de uso. Un rechazo de las reglas sale como {@code 400} con el mensaje en palabras del dueño.
 */
@RestController
@RequestMapping("/v1/grupos-precio-cantidad")
@RequiredArgsConstructor
@Slf4j
public class GrupoPrecioCantidadController {

    private final AdministrarGruposCasoUso grupos;

    @GetMapping
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> listar() {
        LocalDate hoy = LocalDate.now();
        return ejecutar(() -> {
            ResponseGeneric<GrupoPrecioCantidadResponse> r = new ResponseGeneric<>(grupos.listar().stream().map(g -> GrupoPrecioCantidadResponse.de(g, hoy)).toList());
            return r;
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> obtener(@PathVariable int id) {
        return uno(() -> grupos.obtener(id));
    }

    /** El grupo al que pertenece un producto (para Agregar y Actualizar producto); {@code data: null} si no está en ninguno. */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> deProducto(@PathVariable int productoId) {
        return ejecutar(() -> {
            LocalDate hoy = LocalDate.now();
            var g = grupos.grupoDelProducto(productoId);
            ResponseGeneric<GrupoPrecioCantidadResponse> r = new ResponseGeneric<>(g.map(x -> GrupoPrecioCantidadResponse.de(x, hoy)).orElse(null));
            r.setCode(200);
            r.setMensaje(g.isPresent() ? "La peticion fue exitosa" : "El producto no esta en ningun grupo");
            return r;
        });
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> crear(@RequestBody GrupoPrecioCantidadRequest request) {
        return uno(() -> grupos.crear(request.aDominio()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> actualizar(@PathVariable int id, @RequestBody GrupoPrecioCantidadRequest request) {
        return uno(() -> grupos.actualizar(id, request.aDominio()));
    }

    @PutMapping("/{id}/activo")
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> activo(@PathVariable int id, @RequestBody Map<String, Boolean> cuerpo) {
        return uno(() -> {
            grupos.cambiarActivo(id, Boolean.TRUE.equals(cuerpo.get("activo")));
            return grupos.obtener(id);
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> eliminar(@PathVariable int id) {
        return ejecutar(() -> {
            grupos.eliminar(id);
            ResponseGeneric<GrupoPrecioCantidadResponse> r = new ResponseGeneric<>(List.<GrupoPrecioCantidadResponse>of());
            r.setCode(200);
            r.setMensaje("Grupo eliminado");
            return r;
        });
    }

    private ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> uno(Supplier<GrupoPrecioCantidad> accion) {
        LocalDate hoy = LocalDate.now();
        return ejecutar(() -> new ResponseGeneric<>(GrupoPrecioCantidadResponse.de(accion.get(), hoy)));
    }

    private ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> ejecutar(Supplier<ResponseGeneric<GrupoPrecioCantidadResponse>> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (GrupoDePrecioInvalidoException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Grupo de precio por cantidad rechazado por la base: {}", e.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, "Uno de los productos ya está en otro grupo.");
        } catch (RuntimeException e) {
            log.error("Error en grupos de precio por cantidad", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación con el grupo.");
        }
    }

    private static ResponseEntity<ResponseGeneric<GrupoPrecioCantidadResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<GrupoPrecioCantidadResponse> r = new ResponseGeneric<>((GrupoPrecioCantidadResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
