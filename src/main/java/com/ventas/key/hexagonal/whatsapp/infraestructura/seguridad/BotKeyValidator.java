package com.ventas.key.hexagonal.whatsapp.infraestructura.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * [Hexagonal: Driving Adapter — seguridad] [Clean: Frameworks]
 *
 * <p>Valida el encabezado {@code X-Bot-Key} de todo {@code /v1/bot/**} contra {@code bot.api-key} (variable {@code BOT_API_KEY}).
 * Esas rutas son {@code permitAll} en SecurityConfig (el bot no tiene sesion); la llave se valida aqui, igual que el webhook de
 * Facebook valida su firma. Comparacion en tiempo constante. Si la llave no esta configurada en el servidor, TODO se rechaza
 * (cerrado por defecto): mejor un 503 que dejar la puerta abierta.
 */
@Component
public class BotKeyValidator {

    public static final String ENCABEZADO = "X-Bot-Key";

    private final byte[] llaveConfigurada;

    public BotKeyValidator(@Value("${bot.api-key:}") String llave) {
        this.llaveConfigurada = llave == null ? new byte[0] : llave.trim().getBytes(StandardCharsets.UTF_8);
    }

    public void exigir(String llaveRecibida) {
        if (llaveConfigurada.length == 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El bot de WhatsApp no esta configurado en el servidor");
        }
        byte[] recibida = llaveRecibida == null ? new byte[0] : llaveRecibida.trim().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(llaveConfigurada, recibida)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Llave del bot no valida");
        }
    }
}
