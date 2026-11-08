package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarReporteDeCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.ReporteDeCampanaResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Hexagonal: Driving Adapter] Reporte de una campaña y reenvío del código de una persona desde un pedido. Solo administrador (la regla
 * {@code /v1/campanas/**} de {@code SecurityConfig}). Sin lógica.
 */
@RestController
@RequestMapping("/v1/campanas")
@RequiredArgsConstructor
public class CampanaReporteController {

    private final ConsultarReporteDeCampanaCasoUso reporte;
    private final ParticiparEnCampanaCasoUso participar;

    @GetMapping("/{id}/reporte")
    public ResponseEntity<ResponseGeneric<ReporteDeCampanaResponse>> reporte(@PathVariable int id) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ReporteDeCampanaResponse.de(reporte.reporte(id))));
        } catch (CampanaInvalidaException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Vuelve a mandar por correo el código de la persona de este pedido (botón «Enviar código de campaña» de Mis pedidos). */
    @PostMapping("/pedidos/{pedidoId}/enviar-codigo")
    public ResponseEntity<ResponseGeneric<ReporteDeCampanaResponse>> reenviar(@PathVariable int pedidoId) {
        return switch (participar.reenviarCodigo(pedidoId)) {
            case ENVIADO -> {
                ResponseGeneric<ReporteDeCampanaResponse> r = new ResponseGeneric<>(List.<ReporteDeCampanaResponse>of());
                r.setCode(200);
                r.setMensaje("Código enviado");
                yield ResponseEntity.ok(r);
            }
            case SIN_CODIGO -> error(HttpStatus.NOT_FOUND, "Este pedido no tiene un código de campaña.");
            case SIN_CORREO -> error(HttpStatus.BAD_REQUEST, "La persona de este pedido no dejó correo: no hay a dónde mandarle el código.");
        };
    }

    private static ResponseEntity<ResponseGeneric<ReporteDeCampanaResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<ReporteDeCampanaResponse> r = new ResponseGeneric<>((ReporteDeCampanaResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
