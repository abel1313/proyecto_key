package com.ventas.key.hexagonal.datosprueba.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.DatosPruebaException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada.PrepararPruebaCasoUso;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>R16: "Preparar prueba N" en 🧪 Datos de prueba. Mismo permiso que el generador: solo
 * ROLE_ADMIN ({@code /v1/admin/datos-prueba/**} en SecurityConfig) y solo en la base de QA.
 */
@RestController
@RequestMapping("/v1/admin/datos-prueba")
@RequiredArgsConstructor
public class PrepararPruebaController {

    private final PrepararPruebaCasoUso casoUso;

    /** Las pruebas que se pueden preparar, en {@code lista}: [{prueba: "21", titulo: "..."}]. */
    @GetMapping("/escenarios")
    public ResponseEntity<ResponseGeneric<EscenarioDisponible>> escenarios() {
        List<EscenarioDisponible> lista = casoUso.disponibles().stream()
                .map(e -> new EscenarioDisponible(e.prueba(), e.titulo()))
                .toList();
        return ResponseEntity.ok(new ResponseGeneric<>(lista, "Pruebas que se pueden preparar"));
    }

    /** 201 con los pedidos que se crearon. 403 fuera de QA; 400 si la prueba no existe. */
    @PostMapping("/preparar/{prueba}")
    public ResponseEntity<ResponseGeneric<EscenarioPreparado>> preparar(@PathVariable String prueba) {
        int usuarioId = AuthenticationUtils.currentUsuario().getId();
        EscenarioPreparado listo = casoUso.preparar(prueba, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseGeneric<>(listo, "Prueba " + listo.prueba() + " lista"));
    }

    @ExceptionHandler(AmbienteNoPermitidoException.class)
    public ResponseEntity<ResponseGeneric<Void>> ambiente(AmbienteNoPermitidoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, e.getMessage()));
    }

    @ExceptionHandler(DatosPruebaException.class)
    public ResponseEntity<ResponseGeneric<Void>> invalido(DatosPruebaException e) {
        return ResponseEntity.badRequest().body(new ResponseGeneric<>(null, e.getMessage()));
    }

    public record EscenarioDisponible(String prueba, String titulo) {
    }
}
