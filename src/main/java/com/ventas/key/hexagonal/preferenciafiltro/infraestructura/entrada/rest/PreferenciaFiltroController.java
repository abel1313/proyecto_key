package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.entrada.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.SoloPersonalException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.entrada.FiltrosGuardadosCasoUso;
import com.ventas.key.hexagonal.preferenciafiltro.infraestructura.dto.FiltrosGuardadosResponse;
import com.ventas.key.hexagonal.preferenciafiltro.infraestructura.dto.GuardarFiltrosRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>Permiso: ver la pantalla ("tienda/buscar" o "productos/buscar") en SecurityConfig, y además
 * no ser cliente (R1, en el servicio).
 */
@Slf4j
@RestController
@RequestMapping("/v1/preferencias-filtro")
@RequiredArgsConstructor
public class PreferenciaFiltroController {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private final FiltrosGuardadosCasoUso casoUso;
    private final ObjectMapper objectMapper;

    /** 204 si el usuario nunca guardó filtros en esa pantalla. */
    @GetMapping("/{pantalla}")
    public ResponseEntity<ResponseGeneric<FiltrosGuardadosResponse>> obtener(@PathVariable String pantalla) {
        return respuesta(casoUso.obtener(pantalla));
    }

    /** 204 si se mandó {@code {"filtros": {}}}: eso borra lo guardado. */
    @PutMapping("/{pantalla}")
    public ResponseEntity<ResponseGeneric<FiltrosGuardadosResponse>> guardar(
            @PathVariable String pantalla, @RequestBody(required = false) GuardarFiltrosRequest request) {
        Map<String, Object> filtros = request == null || request.filtros() == null ? Map.of() : request.filtros();
        return respuesta(casoUso.guardar(pantalla, aJson(filtros)));
    }

    @DeleteMapping("/{pantalla}")
    public ResponseEntity<Void> borrar(@PathVariable String pantalla) {
        casoUso.borrar(pantalla);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SoloPersonalException.class)
    public ResponseEntity<ResponseGeneric<Object>> soloPersonal(SoloPersonalException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(PreferenciaFiltroException.class)
    public ResponseEntity<ResponseGeneric<Object>> invalida(PreferenciaFiltroException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<ResponseGeneric<FiltrosGuardadosResponse>> respuesta(Optional<FiltrosGuardados> filtros) {
        return filtros
                .map(f -> ResponseEntity.ok(new ResponseGeneric<>(new FiltrosGuardadosResponse(
                        f.pantalla().clave(), aMapa(f), f.actualizado()))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private String aJson(Map<String, Object> filtros) {
        try {
            return objectMapper.writeValueAsString(filtros);
        } catch (JsonProcessingException e) {
            throw PreferenciaFiltroException.noEsObjeto();
        }
    }

    /** Una fila que alguien editó a mano y ya no se puede leer no rompe la pantalla: sale sin filtros. */
    private Map<String, Object> aMapa(FiltrosGuardados f) {
        try {
            return objectMapper.readValue(f.filtrosJson(), MAPA);
        } catch (JsonProcessingException e) {
            log.warn("Filtros guardados ilegibles (usuario {}, {}): {}", f.usuarioId(), f.pantalla().clave(), e.getMessage());
            return Map.of();
        }
    }

    private static ResponseEntity<ResponseGeneric<Object>> error(HttpStatus status, String mensaje) {
        ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
        error.setCode(status.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(status).body(error);
    }
}
