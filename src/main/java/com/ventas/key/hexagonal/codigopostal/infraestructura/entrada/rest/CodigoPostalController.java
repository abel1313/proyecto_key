package com.ventas.key.hexagonal.codigopostal.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.codigopostal.dominio.excepcion.CodigoPostalException;
import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.entrada.BuscarCodigoPostalCasoUso;
import com.ventas.key.mis.productos.dipomex.CodigoPostal;
import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>Mismo URL y mismo JSON de éxito que tenía {@code DopoMexController} (el front lee
 * {@code codigo_postal.municipio}, {@code .estado} y {@code .colonias}); es pública
 * (SecurityConfig: /v1/dipomex/**).
 */
@RestController
@RequestMapping("/v1/dipomex")
@RequiredArgsConstructor
public class CodigoPostalController {

    private final BuscarCodigoPostalCasoUso buscar;

    @GetMapping("/getCodigoPostal/{codigoPostal}")
    public CodigoPostalResponse getCodigoPostal(@PathVariable String codigoPostal) {
        CodigoPostalInfo info = buscar.buscar(codigoPostal);
        CodigoPostal cp = new CodigoPostal();
        cp.setCodigo_postal(info.codigo());
        cp.setMunicipio(info.municipio());
        cp.setEstado(info.estado());
        cp.setColonias(info.colonias());
        return new CodigoPostalResponse(false, null, cp);
    }

    @ExceptionHandler(CodigoPostalException.class)
    public ResponseEntity<ResponseGeneric<Object>> noSePudo(CodigoPostalException e) {
        HttpStatus status = e.motivo() == CodigoPostalException.Motivo.SERVICIO_NO_DISPONIBLE
                ? HttpStatus.BAD_GATEWAY : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new ResponseGeneric<>(null, e.getMessage()));
    }
}
