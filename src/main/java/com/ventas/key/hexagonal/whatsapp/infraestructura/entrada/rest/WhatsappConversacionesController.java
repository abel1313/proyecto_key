package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.ConversacionNoEncontradaException;
import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.EnvioRechazadoException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.AdministrarConversacionesWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.ConversacionResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.MensajeResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.PaginaResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.PlantillaRequest;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.PlantillasResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto.ResponderRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] Lo que usa la pantalla de WhatsApp del panel: {@code /v1/whatsapp/...}.
 * Los GET piden la pantalla {@code admin/whatsapp}; el resto pide además el permiso de escritura (ver
 * {@code SecurityConfig}). Sin lógica: traduce a los casos de uso.
 */
@RestController
@RequestMapping("/v1/whatsapp")
public class WhatsappConversacionesController {

    private final AdministrarConversacionesWhatsappCasoUso conversaciones;

    @Value("${whatsapp.cloud.metodo-pago-activo:false}")
    private boolean metodoDePagoActivo;

    public WhatsappConversacionesController(AdministrarConversacionesWhatsappCasoUso conversaciones) {
        this.conversaciones = conversaciones;
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<ResponseGeneric<PaginaResponse<ConversacionResponse>>> listar(
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "30") int size) {
        var p = conversaciones.listar(pagina, size);
        return ResponseEntity.ok(new ResponseGeneric<>(PaginaResponse.de(p, ConversacionResponse::de)));
    }

    @GetMapping("/conversaciones/{id}")
    public ResponseEntity<ResponseGeneric<ConversacionResponse>> conversacion(@PathVariable long id) {
        try {
            return ok(ConversacionResponse.de(conversaciones.conversacion(id)));
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /** Historial, lo más nuevo primero (el front lo voltea para dibujarlo). */
    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<ResponseGeneric<PaginaResponse<MensajeResponse>>> mensajes(
            @PathVariable long id, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int size) {
        try {
            var p = conversaciones.mensajes(id, pagina, size);
            return ok(PaginaResponse.de(p, MensajeResponse::de));
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/conversaciones/{id}/responder")
    public ResponseEntity<ResponseGeneric<MensajeResponse>> responder(@PathVariable long id,
                                                                      @RequestBody ResponderRequest request) {
        try {
            return ok(MensajeResponse.de(conversaciones.responder(id, request == null ? null : request.texto())));
        } catch (EnvioRechazadoException e) {
            return rechazado(e);
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/conversaciones/{id}/plantilla")
    public ResponseEntity<ResponseGeneric<MensajeResponse>> plantilla(@PathVariable long id,
                                                                      @RequestBody PlantillaRequest request) {
        try {
            var plantilla = new PlantillaWhatsapp(request.nombre(), request.idioma(), request.parametros());
            return ok(MensajeResponse.de(conversaciones.enviarPlantilla(id, plantilla)));
        } catch (EnvioRechazadoException e) {
            return rechazado(e);
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/conversaciones/{id}/tomar")
    public ResponseEntity<ResponseGeneric<ConversacionResponse>> tomar(@PathVariable long id) {
        try {
            return ok(ConversacionResponse.de(conversaciones.tomar(id)));
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/conversaciones/{id}/devolver-bot")
    public ResponseEntity<ResponseGeneric<ConversacionResponse>> devolverAlBot(@PathVariable long id) {
        try {
            return ok(ConversacionResponse.de(conversaciones.devolverAlBot(id)));
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/conversaciones/{id}/atendida")
    public ResponseEntity<ResponseGeneric<ConversacionResponse>> atendida(@PathVariable long id) {
        try {
            return ok(ConversacionResponse.de(conversaciones.marcarAtendida(id)));
        } catch (ConversacionNoEncontradaException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /** Las plantillas propuestas y si ya se pueden enviar (método de pago activo en Meta). */
    @GetMapping("/plantillas")
    public ResponseEntity<ResponseGeneric<PlantillasResponse>> plantillas() {
        return ok(PlantillasResponse.de(metodoDePagoActivo));
    }

    private static <T> ResponseEntity<ResponseGeneric<T>> ok(T data) {
        return ResponseEntity.ok(new ResponseGeneric<>(data));
    }

    private static <T> ResponseEntity<ResponseGeneric<T>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<T> r = new ResponseGeneric<>((T) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }

    /**
     * El mensaje no salió y ya quedó en el historial con su motivo. 409 si fue una regla (ventana de 24 h,
     * sin método de pago); 502 si Meta lo rechazó. El cuerpo trae el mensaje guardado.
     */
    private static ResponseEntity<ResponseGeneric<MensajeResponse>> rechazado(EnvioRechazadoException e) {
        HttpStatus estado = e.registro().estado() == EstadoMensaje.FALLIDO ? HttpStatus.BAD_GATEWAY : HttpStatus.CONFLICT;
        ResponseGeneric<MensajeResponse> r = new ResponseGeneric<>(MensajeResponse.de(e.registro()));
        r.setCode(estado.value());
        r.setMensaje(e.getMessage());
        return ResponseEntity.status(estado).body(r);
    }
}
