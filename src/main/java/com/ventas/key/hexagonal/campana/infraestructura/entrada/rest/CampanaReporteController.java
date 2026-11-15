package com.ventas.key.hexagonal.campana.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarReporteDeCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.EnviarCodigoPedidoRequest;
import com.ventas.key.hexagonal.campana.infraestructura.entrada.rest.dto.ReporteDeCampanaResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Manda por correo el código de campaña de un pedido (botón «Enviar código de campaña» del detalle). El cuerpo es opcional: {@code correo} (si no
     * se manda, el de la persona), y {@code nombre} y {@code telefono} cuando el pedido todavía no tiene persona (se crea). Solo con campaña vigente.
     */
    @PostMapping("/pedidos/{pedidoId}/enviar-codigo")
    public ResponseEntity<ResponseGeneric<ReporteDeCampanaResponse>> reenviar(@PathVariable int pedidoId,
                                                                             @RequestBody(required = false) EnviarCodigoPedidoRequest request) {
        EnviarCodigoPedidoRequest r = request == null ? new EnviarCodigoPedidoRequest() : request;
        return switch (participar.enviarCodigoDePedido(pedidoId, r.getCorreo(), r.getNombre(), r.getTelefono())) {
            case ENVIADO -> {
                ResponseGeneric<ReporteDeCampanaResponse> ok = new ResponseGeneric<>(List.<ReporteDeCampanaResponse>of());
                ok.setCode(200);
                ok.setMensaje("Código enviado");
                yield ResponseEntity.ok(ok);
            }
            case SIN_CAMPANA -> error(HttpStatus.CONFLICT, "No hay una campaña vigente: el código de cliente solo se manda mientras la campaña esté activa.");
            case CORREO_INVALIDO -> error(HttpStatus.BAD_REQUEST, "El correo no es válido. Revisa que tenga la forma nombre@dominio.com");
            case FALTA_CORREO -> error(HttpStatus.BAD_REQUEST, "Escribe el correo al que se manda el código.");
            case FALTA_NOMBRE -> error(HttpStatus.BAD_REQUEST, "Escribe el nombre de la persona para crear su código.");
            case NO_SE_PUDO_ENVIAR -> error(HttpStatus.BAD_GATEWAY, "No se pudo enviar el correo (el servidor de correo no lo aceptó). El código quedó guardado: reintenta en un momento o revisa el correo escrito.");
        };
    }

    private static ResponseEntity<ResponseGeneric<ReporteDeCampanaResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<ReporteDeCampanaResponse> r = new ResponseGeneric<>((ReporteDeCampanaResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
