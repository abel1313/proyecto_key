package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.AdministrarCampanasCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarCampanaActivaCasoUso;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.CampanaRequest;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.CampanaResponse;
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
import java.util.function.Supplier;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>{@code /v1/campanas}: las campañas (eventos con fechas y precios de campaña). Solo administrador (regla propia en {@code SecurityConfig}).
 * Sin lógica: traduce HTTP al caso de uso. Un rechazo de las reglas sale como {@code 400} con el mensaje en palabras del dueño.
 */
@RestController
@RequestMapping("/v1/campanas")
@RequiredArgsConstructor
@Slf4j
public class CampanaController {

    private final AdministrarCampanasCasoUso campanas;
    private final ConsultarCampanaActivaCasoUso activa;

    @GetMapping
    public ResponseEntity<ResponseGeneric<CampanaResponse>> listar() {
        LocalDate hoy = LocalDate.now();
        return ejecutar(() -> new ResponseGeneric<>(campanas.listar().stream().map(c -> CampanaResponse.de(c, hoy)).toList()));
    }

    /** La campaña vigente hoy; {@code data: null} si no hay ninguna (el front no muestra nada de campaña). */
    @GetMapping("/activa")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> vigente() {
        return ejecutar(() -> {
            LocalDate hoy = LocalDate.now();
            var c = activa.vigente();
            ResponseGeneric<CampanaResponse> r = new ResponseGeneric<>(c.map(x -> CampanaResponse.de(x, hoy)).orElse(null));
            r.setCode(200);
            r.setMensaje(c.isPresent() ? "La peticion fue exitosa" : "No hay campaña activa");
            return r;
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> obtener(@PathVariable int id) {
        return uno(() -> campanas.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<CampanaResponse>> crear(@RequestBody CampanaRequest request) {
        return uno(() -> campanas.crear(request.aDominio()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> actualizar(@PathVariable int id, @RequestBody CampanaRequest request) {
        return uno(() -> campanas.actualizar(id, request.aDominio()));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> activar(@PathVariable int id) {
        return uno(() -> campanas.activar(id));
    }

    @PutMapping("/{id}/apagar")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> apagar(@PathVariable int id) {
        return uno(() -> campanas.apagar(id));
    }

    @PostMapping("/{id}/duplicar")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> duplicar(@PathVariable int id) {
        return uno(() -> campanas.duplicar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<CampanaResponse>> eliminar(@PathVariable int id) {
        return ejecutar(() -> {
            campanas.eliminar(id);
            ResponseGeneric<CampanaResponse> r = new ResponseGeneric<>(List.<CampanaResponse>of());
            r.setCode(200);
            r.setMensaje("Campaña eliminada");
            return r;
        });
    }

    private ResponseEntity<ResponseGeneric<CampanaResponse>> uno(Supplier<Campana> accion) {
        LocalDate hoy = LocalDate.now();
        return ejecutar(() -> new ResponseGeneric<>(CampanaResponse.de(accion.get(), hoy)));
    }

    private ResponseEntity<ResponseGeneric<CampanaResponse>> ejecutar(Supplier<ResponseGeneric<CampanaResponse>> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (CampanaInvalidaException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Campaña rechazada por la base: {}", e.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, "No se pudo guardar la campaña: revisa que los productos no estén repetidos.");
        } catch (RuntimeException e) {
            log.error("Error en campañas", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación con la campaña.");
        }
    }

    private static ResponseEntity<ResponseGeneric<CampanaResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<CampanaResponse> r = new ResponseGeneric<>((CampanaResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
