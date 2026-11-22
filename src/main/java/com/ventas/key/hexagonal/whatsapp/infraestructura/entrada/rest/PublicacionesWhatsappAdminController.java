package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.PublicacionWhatsappException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PublicacionesAdminCasoUso;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.PublicacionWhatsappRequest;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.PublicacionWhatsappResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>{@code /v1/whatsapp/publicaciones}: la pantalla del panel para publicar en grupos de WhatsApp. Solo administrador (regla propia en
 * {@code SecurityConfig}). Sin logica: traduce HTTP a casos de uso. Un rechazo de las reglas sale con su codigo (400, 404 o 409) y el
 * mensaje en palabras del dueño. El bot usa otras rutas ({@link BotPublicacionesController}).
 */
@RestController
@RequestMapping("/v1/whatsapp/publicaciones")
@RequiredArgsConstructor
@Slf4j
public class PublicacionesWhatsappAdminController {

    private final PublicacionesAdminCasoUso publicaciones;

    @GetMapping
    public ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> listar() {
        return ejecutar(() -> {
            List<PublicacionWhatsappResponse> lista = publicaciones.listar().stream().map(PublicacionWhatsappResponse::de).toList();
            ResponseGeneric<PublicacionWhatsappResponse> r = new ResponseGeneric<>(lista);
            r.setCode(200); // sin publicaciones no es un error: la lista sale vacia
            r.setMensaje(lista.isEmpty() ? "Todavía no hay publicaciones de WhatsApp" : "La peticion fue exitosa");
            return r;
        });
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> crear(@RequestBody PublicacionWhatsappRequest request) {
        return uno(() -> publicaciones.crear(request.aDominio()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> cancelar(@PathVariable int id) {
        return uno(() -> publicaciones.cancelar(id));
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> reintentar(@PathVariable int id) {
        return uno(() -> publicaciones.reintentar(id));
    }

    private ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> uno(Supplier<PublicacionWhatsapp> accion) {
        return ejecutar(() -> new ResponseGeneric<>(PublicacionWhatsappResponse.de(accion.get())));
    }

    private ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> ejecutar(Supplier<ResponseGeneric<PublicacionWhatsappResponse>> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (PublicacionWhatsappException e) {
            HttpStatus estado = e.invalida() ? HttpStatus.BAD_REQUEST : e.noExiste() ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
            return error(estado, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Publicación de WhatsApp rechazada por la base: {}", e.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, "No se pudo guardar la publicación: revisa que los datos no sean demasiado largos.");
        } catch (RuntimeException e) {
            log.error("Error en publicaciones de WhatsApp", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación con la publicación.");
        }
    }

    private static ResponseEntity<ResponseGeneric<PublicacionWhatsappResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<PublicacionWhatsappResponse> r = new ResponseGeneric<>((PublicacionWhatsappResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
