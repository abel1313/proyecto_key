package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.PublicacionWhatsappException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PublicacionesParaElBotCasoUso;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.ErrorPublicacionRequest;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.PublicacionParaElBotResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.seguridad.BotKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>Endpoints del bot de WhatsApp (repo whatsapp_bot). Sin sesion: se validan con {@code X-Bot-Key} ({@link BotKeyValidator}).
 * Sin logica: traduce HTTP a casos de uso.
 */
@RestController
@RequestMapping("/v1/bot/publicaciones")
@RequiredArgsConstructor
public class BotPublicacionesController {

    private final PublicacionesParaElBotCasoUso casoUso;
    private final BotKeyValidator llave;

    /** Las que ya les toca salir; quedan EN_PROCESO (no se vuelven a entregar). Lista vacia si no hay. */
    @GetMapping("/pendientes")
    public List<PublicacionParaElBotResponse> pendientes(@RequestHeader(value = BotKeyValidator.ENCABEZADO, required = false) String key) {
        llave.exigir(key);
        return casoUso.recogerPendientes().stream().map(this::aRespuesta).toList();
    }

    /** JPEG liviano (binario, no base64). 404 si no hay imagen: el bot publica solo el texto. */
    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable int id,
                                         @RequestHeader(value = BotKeyValidator.ENCABEZADO, required = false) String key) {
        llave.exigir(key);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(casoUso.imagenDe(id));
    }

    @PostMapping("/{id}/publicada")
    public ResponseEntity<Void> publicada(@PathVariable int id,
                                          @RequestHeader(value = BotKeyValidator.ENCABEZADO, required = false) String key) {
        llave.exigir(key);
        casoUso.marcarPublicada(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/error")
    public ResponseEntity<Void> error(@PathVariable int id, @RequestBody(required = false) ErrorPublicacionRequest cuerpo,
                                      @RequestHeader(value = BotKeyValidator.ENCABEZADO, required = false) String key) {
        llave.exigir(key);
        casoUso.marcarError(id, cuerpo == null ? null : cuerpo.detalle());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(PublicacionWhatsappException.class)
    public ResponseEntity<String> regla(PublicacionWhatsappException e) {
        return ResponseEntity.status(e.noExiste() ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT).body(e.getMessage());
    }

    private PublicacionParaElBotResponse aRespuesta(PublicacionWhatsapp p) {
        String imagen = p.imagenId() == null ? null : "/v1/bot/publicaciones/" + p.id() + "/imagen";
        return new PublicacionParaElBotResponse(p.id(), p.grupo(), p.texto(), imagen, p.codigoRef(), p.modoVenta().name());
    }
}
