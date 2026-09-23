package com.ventas.key.hexagonal.rifa.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.rifa.dominio.excepcion.CargaBoletosException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.UrlParticipacionDuplicadaException;
import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.ModoDeCarga;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;
import com.ventas.key.hexagonal.rifa.dominio.puerto.entrada.GestionarBoletosCasoUso;
import com.ventas.key.hexagonal.rifa.infraestructura.dto.CargarBoletosRequest;
import com.ventas.key.hexagonal.rifa.infraestructura.dto.GrupoBoletosResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

/**
 * Boletos de rifa cargados por participacion en redes, agrupados por (plataforma + perfil).
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Convive con {@code BoletoRifaControllerImpl}, que sigue sirviendo el formato viejo de
 * filas sueltas: por la decision D1 los boletos ya cargados no se migran. Los dos leen las
 * mismas filas de {@code boletos_rifa}; lo unico que cambia es como se presentan.
 *
 * <p>Los endpoints estan protegidos por acciones configurables: ver
 * {@code migration_accion_rifa_boletos_agrupados.sql} y los matchers en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/v1/rifas")
@Slf4j
@RequiredArgsConstructor
public class BoletoAgrupadoController {

    private final GestionarBoletosCasoUso gestionarBoletos;

    /** La pantalla: un renglon por perfil, con sus participaciones y su total de boletos. */
    @GetMapping("/{rifaId}/boletos-agrupados")
    public ResponseEntity<Object> verAgrupados(@PathVariable Integer rifaId) {
        List<GrupoBoletosResponse> grupos = gestionarBoletos.verAgrupados(rifaId).stream()
                .map(GrupoBoletosResponse::de)
                .toList();
        // El tipo explicito importa: con new ResponseGeneric<>(lista) Java elige el constructor
        // de `lista` y deja `data` en null. El front lee `data`, y la pantalla salia vacia
        // aunque la rifa tuviera boletos.
        return ResponseEntity.ok(new ResponseGeneric<List<GrupoBoletosResponse>>(grupos));
    }

    /**
     * Alta de un perfil con todas sus participaciones de una sola pasada. Cada URL es un
     * boleto.
     *
     * <p>Todo o nada: si una URL choca con otra ya cargada, no entra ninguna.</p>
     */
    @PostMapping("/{rifaId}/boletos-agrupados")
    public ResponseEntity<Object> cargar(@PathVariable Integer rifaId,
                                         @RequestBody CargarBoletosRequest request) {
        return ejecutar(() -> gestionarBoletos.cargar(
                new GestionarBoletosCasoUso.CargaDeParticipaciones(
                        rifaId,
                        request.getConcursanteId(),
                        request.getPlataforma(),
                        request.getUrlPerfil(),
                        aNuevasParticipaciones(request))));
    }

    /** Suma una participacion a un grupo que ya existe, sin recargar la cabecera (R3). */
    @PostMapping("/{rifaId}/boletos-agrupados/participaciones")
    public ResponseEntity<Object> agregarParticipacion(
            @PathVariable Integer rifaId,
            @RequestParam Plataforma plataforma,
            @RequestParam String urlPerfil,
            @RequestBody CargarBoletosRequest.ParticipacionRequest request) {
        return ejecutar(() -> gestionarBoletos.agregarParticipacion(rifaId, plataforma, urlPerfil,
                new GestionarBoletosCasoUso.NuevaParticipacion(
                        request.getUrlParticipacion(),
                        request.getMotivo(),
                        ModoDeCarga.oPorDefecto(request.getModo()))));
    }

    /**
     * Corrige la URL de la publicacion o lo que hizo, en un boleto ya cargado. No cambia
     * cuantos boletos tiene la persona.
     */
    @PutMapping("/{rifaId}/boletos-agrupados/participaciones/{boletoId}")
    public ResponseEntity<Object> editarParticipacion(@PathVariable Integer rifaId,
                                                      @PathVariable Integer boletoId,
                                                      @RequestBody CargarBoletosRequest.ParticipacionRequest request) {
        return ejecutar(() -> gestionarBoletos.editarParticipacion(rifaId, boletoId,
                new GestionarBoletosCasoUso.NuevaParticipacion(
                        request.getUrlParticipacion(),
                        request.getMotivo(),
                        ModoDeCarga.oPorDefecto(request.getModo()))));
    }

    /** Quita una participacion. El grupo puede quedar vacio y se devuelve igual. */
    @DeleteMapping("/{rifaId}/boletos-agrupados/participaciones/{boletoId}")
    public ResponseEntity<Object> quitarParticipacion(@PathVariable Integer rifaId,
                                                      @PathVariable Integer boletoId) {
        return ejecutar(() -> gestionarBoletos.quitarParticipacion(rifaId, boletoId));
    }

    /**
     * Un solo lugar donde se mapean las excepciones del dominio a status, para que los
     * cuatro endpoints contesten igual.
     *
     * <p>El <b>409</b> del duplicado no es un error de programacion: es "esta url ya es de
     * alguien, decidi vos". El front la puede volver a mandar con
     * {@code modo: "REPETIDA_PERMITIDA"} si de verdad se repite.</p>
     */
    private ResponseEntity<Object> ejecutar(Supplier<GrupoDeBoletos> operacion) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(GrupoBoletosResponse.de(operacion.get())));
        } catch (UrlParticipacionDuplicadaException e) {
            log.info("Rifa: url de participacion duplicada -> {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ResponseGeneric<>(e.getMessage()));
        } catch (GrupoNoEncontradoException e) {
            log.info("Rifa: grupo no encontrado -> {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseGeneric<>(e.getMessage()));
        } catch (CargaBoletosException e) {
            log.info("Rifa: carga rechazada -> {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseGeneric<>(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Rifa: error inesperado cargando boletos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<>("No se pudo completar la operacion"));
        }
    }

    private List<GestionarBoletosCasoUso.NuevaParticipacion> aNuevasParticipaciones(
            CargarBoletosRequest request) {
        if (request.getParticipaciones() == null) {
            return List.of();
        }
        return request.getParticipaciones().stream()
                .map(p -> new GestionarBoletosCasoUso.NuevaParticipacion(
                        p.getUrlParticipacion(), p.getMotivo(), ModoDeCarga.oPorDefecto(p.getModo())))
                .toList();
    }
}
