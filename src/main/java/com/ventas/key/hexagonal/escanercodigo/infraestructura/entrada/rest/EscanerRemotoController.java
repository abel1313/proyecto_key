package com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada.EscanerRemotoCasoUso;
import com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest.dto.EscanerResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private final EscanerRemotoCasoUso escaner;

    @PostMapping("/sesiones")
    public ResponseEntity<ResponseGeneric<EscanerResponse.Sesion>> abrir(HttpServletRequest request) {
        return responder(() -> EscanerResponse.Sesion.de(escaner.abrir(ipDe(request))));
    }

    @PostMapping("/sesiones/{id}/codigos")
    public ResponseEntity<ResponseGeneric<Boolean>> enviar(@PathVariable String id, @RequestBody EscanerResponse.EnviarCodigo cuerpo) {
        return responder(() -> {
            escaner.enviar(id, cuerpo == null ? null : cuerpo.codigo());
            return Boolean.TRUE;
        });
    }

    @GetMapping("/sesiones/{id}/codigos")
    public ResponseEntity<ResponseGeneric<EscanerResponse.Lectura>> leer(@PathVariable String id,
                                                                         @RequestParam(defaultValue = "0") long desde) {
        return responder(() -> EscanerResponse.Lectura.de(escaner.leer(id, desde)));
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
        } catch (EscanerException.SesionNoDisponible e) {
            return error(HttpStatus.GONE, e.getMessage());
        } catch (EscanerException.DemasiadasSolicitudes e) {
            return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (EscanerException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error en el escáner remoto", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar. Intenta de nuevo.");
        }
    }

    private static <T> ResponseEntity<ResponseGeneric<T>> error(HttpStatus status, String mensaje) {
        ResponseGeneric<T> cuerpo = new ResponseGeneric<>((T) null);
        cuerpo.setCode(status.value());
        cuerpo.setMensaje(mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }

    /**
     * La IP de la PC, solo para limitar conexiones abiertas. Detrás del proxy llega en X-Forwarded-For; se toma la de más a la
     * DERECHA (la que agregó el proxy de confianza), igual que AuthController: la de la izquierda la puede inventar el cliente.
     */
    private static String ipDe(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            String[] ips = reenviada.split(",");
            return ips[ips.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
