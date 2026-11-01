package com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada.ConsultarGraficasCasoUso;
import com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest.dto.GraficasResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Gráficas del Dashboard. Todo {@code /v1/dashboard/**} ya es solo administrador en {@code SecurityConfig}, así que no
 * necesita regla nueva. Solo lectura y sin lógica: traduce HTTP al caso de uso.
 */
@RestController
@RequestMapping("/v1/dashboard/graficas")
@RequiredArgsConstructor
@Slf4j
public class GraficasVentasController {

    private final ConsultarGraficasCasoUso graficas;

    @GetMapping("/ventas-por-forma-pago")
    public ResponseEntity<ResponseGeneric<GraficasResponse.PorFormaDePago>> porFormaDePago(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return responder(() -> GraficasResponse.PorFormaDePago.de(graficas.ventasPorFormaDePago(desde, hasta)));
    }

    @GetMapping("/ventas-gastos-mensual")
    public ResponseEntity<ResponseGeneric<List<GraficasResponse.Mes>>> ventasGastosMensual(
            @RequestParam(defaultValue = "12") int meses) {
        return responder(() -> graficas.ventasYGastosPorMes(meses).stream().map(GraficasResponse.Mes::de).toList());
    }

    @GetMapping("/por-cobrar")
    public ResponseEntity<ResponseGeneric<GraficasResponse.PorCobrar>> porCobrar() {
        return responder(() -> GraficasResponse.PorCobrar.de(graficas.dineroPorCobrar()));
    }

    @GetMapping("/capital-parado")
    public ResponseEntity<ResponseGeneric<GraficasResponse.Parado>> capitalParado(@RequestParam(defaultValue = "60") int dias) {
        return responder(() -> GraficasResponse.Parado.de(graficas.capitalParado(dias)));
    }

    @GetMapping("/pedidos-por-estado")
    public ResponseEntity<ResponseGeneric<GraficasResponse.PedidosPorEstado>> pedidosPorEstado(@RequestParam(defaultValue = "90") int dias) {
        return responder(() -> GraficasResponse.PedidosPorEstado.de(graficas.pedidosPorEstado(dias)));
    }

    private <T> ResponseEntity<ResponseGeneric<T>> responder(Supplier<T> consulta) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(consulta.get()));
        } catch (GraficasVentasException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error en las gráficas del dashboard", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar las gráficas.");
        }
    }

    private static <T> ResponseEntity<ResponseGeneric<T>> error(HttpStatus status, String mensaje) {
        ResponseGeneric<T> error = new ResponseGeneric<>((T) null);
        error.setCode(status.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(status).body(error);
    }
}
