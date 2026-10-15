package com.ventas.key.hexagonal.datoslegales.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.datoslegales.dominio.excepcion.DatosLegalesInvalidosException;
import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;
import com.ventas.key.hexagonal.datoslegales.dominio.puerto.entrada.DatosLegalesCasoUso;
import com.ventas.key.hexagonal.datoslegales.infraestructura.dto.DatosLegalesRequest;
import com.ventas.key.hexagonal.datoslegales.infraestructura.dto.DatosLegalesResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>GET público (lo leen el pie de página, Términos, Aviso de privacidad y el ticket) y PUT con
 * Escritura en Configuración del negocio ({@code admin/negocio}), ver SecurityConfig.
 */
@RestController
@RequestMapping("/v1/datos-legales")
@RequiredArgsConstructor
public class DatosLegalesController {

    private final DatosLegalesCasoUso casoUso;

    @GetMapping
    public ResponseEntity<ResponseGeneric<DatosLegalesResponse>> consultar() {
        return ResponseEntity.ok(new ResponseGeneric<>(DatosLegalesResponse.de(casoUso.consultar())));
    }

    @PutMapping
    public ResponseEntity<Object> guardar(@RequestBody DatosLegalesRequest r) {
        try {
            DatosLegales guardados = casoUso.guardar(new DatosLegales(r.nombreResponsable(), r.rfc(),
                    r.domicilio(), r.telefono(), r.correo(), r.horarioAtencion()));
            return ResponseEntity.ok(new ResponseGeneric<>(DatosLegalesResponse.de(guardados)));
        } catch (DatosLegalesInvalidosException e) {
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
