package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.CanalWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.ResponderMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.MensajeWhatsappRequest;
import com.ventas.key.hexagonal.whatsapp.infraestructura.dto.RespuestaBotResponse;
import com.ventas.key.hexagonal.whatsapp.infraestructura.seguridad.BotKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>El bot de WhatsApp (repo whatsapp_bot) pregunta aqui que hacer con cada mensaje. Sin sesion: se valida {@code X-Bot-Key}.
 * Sin logica: traduce HTTP al caso de uso. Siempre contesta 200 con una accion (IGNORAR incluida): el bot no tiene que adivinar.
 */
@RestController
@RequestMapping("/v1/bot/whatsapp")
@RequiredArgsConstructor
public class BotWhatsappController {

    private final ResponderMensajeWhatsappCasoUso casoUso;
    private final BotKeyValidator llave;

    @PostMapping("/mensaje")
    public RespuestaBotResponse mensaje(@RequestBody MensajeWhatsappRequest cuerpo,
                                        @RequestHeader(value = BotKeyValidator.ENCABEZADO, required = false) String key) {
        llave.exigir(key);
        if (CanalWhatsapp.desde(cuerpo.canal()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El canal debe ser GRUPO o PRIVADO");
        }
        return RespuestaBotResponse.de(casoUso.atender(cuerpo.aDominio()));
    }
}
