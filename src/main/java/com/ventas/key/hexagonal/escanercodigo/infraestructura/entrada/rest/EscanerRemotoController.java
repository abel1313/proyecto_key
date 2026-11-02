package com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EsperaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio.EscanerRemotoService;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada.EscanerRemotoCasoUso;
import com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest.dto.EscanerResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Escanear un código de barras con el celular y recibirlo en la PC. Endpoints PÚBLICOS (el buscador de la tienda también los usa):
 * lo único que protege cada conexión es su identificador aleatorio, que solo viaja en el QR. Sin lógica: traduce HTTP al caso de uso.
 */
@RestController
@RequestMapping("/v1/escaner")
@RequiredArgsConstructor
@Slf4j
public class EscanerRemotoController {

    /** Cuánto se queda abierta una pregunta de la PC antes de contestar «nada todavía» (menos que el límite de 30 s del servidor). */
    static final long ESPERA_MAXIMA_MS = 25_000;

    private final EscanerRemotoCasoUso escaner;

    @PostMapping("/sesiones")
    public ResponseEntity<ResponseGeneric<EscanerResponse.Sesion>> abrir(HttpServletRequest request) {
        return responder(() -> EscanerResponse.Sesion.de(escaner.abrir(origenDe(request))));
    }

    /** El celular manda un código. {@code data} = si la PC lo estaba esperando (false: se guardó, pero nadie lo pedía en ese momento). */
    @PostMapping("/sesiones/{id}/codigos")
    public ResponseEntity<ResponseGeneric<Boolean>> enviar(@PathVariable String id, @RequestBody EscanerResponse.EnviarCodigo cuerpo) {
        return responder(() -> escaner.enviar(id, cuerpo == null ? null : cuerpo.codigo()));
    }

    /**
     * La PC pregunta si llegó un código. Con {@code esperar=true} la pregunta se queda abierta hasta que el celular mande uno (o pasen
     * 25 s, y entonces contesta vacío para que la PC vuelva a preguntar): el código llega en milisegundos, sin consultar cada segundo.
     * {@link DeferredResult} libera el hilo del servidor mientras espera, así que muchas PCs esperando no lo agotan.
     */
    @GetMapping("/sesiones/{id}/codigos")
    public DeferredResult<ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>>> leer(
            @PathVariable String id, @RequestParam(defaultValue = "0") long desde, @RequestParam(defaultValue = "false") boolean esperar) {
        return leerOEsperar(id, desde, esperar);
    }

    private DeferredResult<ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>>> leerOEsperar(String id, long desde, boolean esperar) {
        DeferredResult<ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>>> resultado = new DeferredResult<>(ESPERA_MAXIMA_MS);
        if (!esperar) {
            resultado.setResult(responder(() -> EscanerResponse.Lectura.de(escaner.leer(id, desde))));
            return resultado;
        }
        EsperaDeCodigos espera;
        try {
            espera = escaner.esperar(id, desde);
        } catch (RuntimeException e) {
            resultado.setResult(aRespuesta(e));
            return resultado;
        }
        resultado.onCompletion(() -> espera.cancelar().run());   // la PC se fue o ya se contestó: se quita la pregunta abierta
        resultado.onTimeout(() -> resultado.setResult(responder(() -> EscanerResponse.Lectura.de(escaner.leer(id, desde)))));
        espera.futuro().whenComplete((lectura, error) -> resultado.setResult(error == null
                ? ResponseEntity.ok(new ResponseGeneric<>(EscanerResponse.Lectura.de(lectura)))
                : aRespuesta(error)));
        return resultado;
    }

    @GetMapping("/sesiones/{id}")
    public ResponseEntity<ResponseGeneric<EscanerResponse.Estado>> estado(@PathVariable String id) {
        return responder(() -> EscanerResponse.Estado.de(escaner.estado(id)));
    }

    /** El celular avisa que sigue conectado (cada ~15 s). */
    @PostMapping("/sesiones/{id}/latido")
    public ResponseEntity<ResponseGeneric<Boolean>> latido(@PathVariable String id) {
        return responder(() -> {
            escaner.latido(id);
            return Boolean.TRUE;
        });
    }

    // ─────────── Canal de la CUENTA (con sesión iniciada): la PC y el celular de la misma persona se encuentran solos, sin QR ───────────

    /** ¿Está conectado el celular de mi cuenta? */
    @GetMapping("/canal")
    public ResponseEntity<ResponseGeneric<EscanerResponse.Estado>> estadoDelCanal() {
        return responder(() -> EscanerResponse.Estado.de(escaner.estado(escaner.idDelCanal(usuarioActual()))));
    }

    /** La PC de mi cuenta deja la pregunta abierta (igual que con una conexión por QR). */
    @GetMapping("/canal/codigos")
    public DeferredResult<ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>>> leerDelCanal(
            @RequestParam(defaultValue = "0") long desde, @RequestParam(defaultValue = "false") boolean esperar) {
        String id;
        try {
            id = escaner.idDelCanal(usuarioActual());
        } catch (RuntimeException e) {
            DeferredResult<ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>>> r = new DeferredResult<>();
            r.setResult(aRespuesta(e));
            return r;
        }
        return leerOEsperar(id, desde, esperar);
    }

    /** El celular de mi cuenta avisa que sigue ahí. */
    @PostMapping("/canal/latido")
    public ResponseEntity<ResponseGeneric<Boolean>> latidoDelCanal() {
        return responder(() -> {
            escaner.latido(escaner.idDelCanal(usuarioActual()));
            return Boolean.TRUE;
        });
    }

    /** El celular de mi cuenta manda un código. {@code data} = si la PC lo estaba esperando. */
    @PostMapping("/canal/codigos")
    public ResponseEntity<ResponseGeneric<Boolean>> enviarAlCanal(@RequestBody EscanerResponse.EnviarCodigo cuerpo) {
        return responder(() -> escaner.enviar(escaner.idDelCanal(usuarioActual()), cuerpo == null ? null : cuerpo.codigo()));
    }

    /** El usuario con sesión; sin sesión no se tiene canal (el servidor ya exige sesión en estas rutas, esto es la red de seguridad). */
    private static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken || auth.getName() == null) {
            return null;
        }
        return auth.getName();
    }

    @DeleteMapping("/sesiones/{id}")
    public ResponseEntity<ResponseGeneric<Boolean>> cerrar(@PathVariable String id) {
        return responder(() -> {
            escaner.cerrar(id);
            return Boolean.TRUE;
        });
    }

    private <T> ResponseEntity<ResponseGeneric<T>> responder(Supplier<T> accion) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(accion.get()));
        } catch (RuntimeException e) {
            return aRespuesta(e);
        }
    }

    /** Traduce lo que falló a la respuesta HTTP (también lo que llega envuelto de una pregunta abierta). */
    private <T> ResponseEntity<ResponseGeneric<T>> aRespuesta(Throwable falla) {
        Throwable causa = falla instanceof CompletionException && falla.getCause() != null ? falla.getCause() : falla;
        if (causa instanceof EscanerException.SesionNoDisponible) {
            return error(HttpStatus.GONE, causa.getMessage());
        }
        if (causa instanceof EscanerException.DemasiadasSolicitudes) {
            return error(HttpStatus.TOO_MANY_REQUESTS, causa.getMessage());
        }
        if (causa instanceof EscanerException) {
            return error(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        log.error("Error en el escáner remoto", causa);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar. Intenta de nuevo.");
    }

    private static <T> ResponseEntity<ResponseGeneric<T>> error(HttpStatus status, String mensaje) {
        ResponseGeneric<T> cuerpo = new ResponseGeneric<>((T) null);
        cuerpo.setCode(status.value());
        cuerpo.setMensaje(mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }

    /**
     * Quién abre la conexión, solo para limitar cuántas tiene abiertas. Con sesión (el personal) cuenta por usuario: no gasta el cupo
     * de la IP de la tienda, que comparten todas las PCs y los celulares detrás del mismo NAT. Sin sesión cuenta por IP.
     */
    private static String origenDe(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken) && auth.getName() != null) {
            return EscanerRemotoService.PREFIJO_USUARIO + auth.getName();
        }
        return EscanerRemotoService.PREFIJO_IP + ipDe(request);
    }

    /**
     * La IP, de la entrada de más a la DERECHA de X-Forwarded-For (la que agregó el proxy de confianza), igual que AuthController: la
     * de la izquierda la puede inventar el cliente. Las IPv6 se agrupan por su prefijo /48: un proveedor o un servidor reparte miles de
     * direcciones /64, y contar cada una por separado dejaría a un atacante con miles de «orígenes» distintos.
     */
    static String ipDe(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        String ip;
        if (reenviada != null && !reenviada.isBlank()) {
            String[] ips = reenviada.split(",");
            ip = ips[ips.length - 1].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return agruparIpv6(ip);
    }

    /** {@code 2001:db8:1:2:aaaa:bbbb:cccc:dddd} → {@code 2001:db8:1::/48}. Una IPv4 se deja igual. */
    static String agruparIpv6(String ip) {
        if (ip == null || !ip.contains(":")) {
            return ip;
        }
        try {
            byte[] bytes = java.net.InetAddress.getByName(ip).getAddress();
            if (bytes.length != 16) {
                return ip;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i += 2) {
                sb.append(String.format("%x", ((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff))).append(i < 4 ? ":" : "");
            }
            return sb + "::/48";
        } catch (java.net.UnknownHostException | SecurityException e) {
            return ip;
        }
    }
}
