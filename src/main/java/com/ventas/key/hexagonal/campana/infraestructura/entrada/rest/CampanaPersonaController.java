package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.VerificarCodigoRequest;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.VerificarCodigoResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] {@code /v1/campanas/personas}: verificar un código en Venta directa y borrar a una persona. Solo administrador
 * (la regla {@code /v1/campanas/**} de {@code SecurityConfig}). Sin lógica.
 */
@RestController
@RequestMapping("/v1/campanas/personas")
@RequiredArgsConstructor
public class CampanaPersonaController {

    private final ParticiparEnCampanaCasoUso participar;

    @PostMapping("/verificar")
    public ResponseEntity<ResponseGeneric<VerificarCodigoResponse>> verificar(@RequestBody VerificarCodigoRequest request, Authentication auth) {
        try {
            var v = participar.verificar(auth == null ? null : auth.getName(), request.codigo(),
                    new ContactoCliente(null, request.correo(), request.telefono()));
            return ResponseEntity.ok(new ResponseGeneric<>(VerificarCodigoResponse.de(v)));
        } catch (CampanaInvalidaException e) {
            return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<VerificarCodigoResponse>> eliminar(@PathVariable int id) {
        participar.eliminarPersona(id);
        ResponseGeneric<VerificarCodigoResponse> r = new ResponseGeneric<>(List.<VerificarCodigoResponse>of());
        r.setCode(200);
        r.setMensaje("Datos de la persona eliminados");
        return ResponseEntity.ok(r);
    }

    private static ResponseEntity<ResponseGeneric<VerificarCodigoResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<VerificarCodigoResponse> r = new ResponseGeneric<>((VerificarCodigoResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
