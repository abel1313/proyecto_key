package com.ventas.key.hexagonal.datosprueba.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.DatosPruebaException;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.GeneracionEnCursoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.Avance;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada.DatosPruebaCasoUso;
import com.ventas.key.hexagonal.datosprueba.infraestructura.dto.GenerarDatosPruebaRequest;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>Permiso: solo ROLE_ADMIN (R2), regla propia en SecurityConfig antes de la de /v1/admin/**.
 */
@RestController
@RequestMapping("/v1/admin/datos-prueba")
@RequiredArgsConstructor
public class DatosPruebaController {

    static final int MODELOS_POR_DEFECTO = 20_000;
    static final int PEDIDOS_POR_DEFECTO = 1_000;

    private final DatosPruebaCasoUso casoUso;

    /** 202: arranco y sigue en segundo plano. El avance se consulta con GET /avance. */
    @PostMapping("/generar")
    public ResponseEntity<ResponseGeneric<Avance>> generar(@RequestBody(required = false) GenerarDatosPruebaRequest req) {
        GenerarDatosPruebaRequest r = req != null ? req : new GenerarDatosPruebaRequest(null, null, null, null, null);
        PlanDeDatos plan = new PlanDeDatos(
                r.modelos() != null ? r.modelos() : MODELOS_POR_DEFECTO,
                r.articulosMin() != null ? r.articulosMin() : 1,
                r.articulosMax() != null ? r.articulosMax() : PlanDeDatos.MAX_ARTICULOS_POR_MODELO,
                r.pedidos() != null ? r.pedidos() : PEDIDOS_POR_DEFECTO,
                r.semilla() != null ? r.semilla() : System.currentTimeMillis());
        int usuarioId = AuthenticationUtils.currentUsuario().getId();
        Avance avance = casoUso.iniciar(plan, usuarioId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ResponseGeneric<>(avance, "Se empezaron a generar los datos de prueba en segundo plano"));
    }

    @GetMapping("/avance")
    public ResponseEntity<ResponseGeneric<Avance>> avance() {
        return ResponseEntity.ok(new ResponseGeneric<>(casoUso.avance()));
    }

    @PostMapping("/dar-de-baja")
    public ResponseEntity<ResponseGeneric<Integer>> darDeBaja() {
        int articulos = casoUso.darDeBaja();
        return ResponseEntity.ok(new ResponseGeneric<>(articulos,
                articulos + " artículos de prueba dados de baja (y sus modelos)"));
    }

    @ExceptionHandler(AmbienteNoPermitidoException.class)
    public ResponseEntity<ResponseGeneric<Void>> ambiente(AmbienteNoPermitidoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, e.getMessage()));
    }

    @ExceptionHandler(GeneracionEnCursoException.class)
    public ResponseEntity<ResponseGeneric<Void>> enCurso(GeneracionEnCursoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ResponseGeneric<>(null, e.getMessage()));
    }

    @ExceptionHandler(DatosPruebaException.class)
    public ResponseEntity<ResponseGeneric<Void>> invalido(DatosPruebaException e) {
        return ResponseEntity.badRequest().body(new ResponseGeneric<>(null, e.getMessage()));
    }
}
