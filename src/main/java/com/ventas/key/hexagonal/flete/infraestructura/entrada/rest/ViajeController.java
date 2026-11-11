package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AdministrarViajesCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.ConsultarReporteDeViajeCasoUso;
import com.ventas.key.hexagonal.flete.infraestructura.salida.archivo.ViajeExcelGenerador;
import com.ventas.key.hexagonal.flete.infraestructura.salida.archivo.ViajePdfGenerador;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.ReporteDeViajeResponse;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.ViajeRequest;
import com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto.ViajeResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
 * [Hexagonal: Driving Adapter] {@code /v1/fletes}: los viajes de compra (flete, gastos y productos que llegaron). Solo administrador (regla propia en
 * {@code SecurityConfig}). Sin lógica: traduce HTTP al caso de uso; un rechazo de las reglas sale como {@code 400} con el mensaje en palabras del dueño.
 */
@RestController
@RequestMapping("/v1/fletes")
@RequiredArgsConstructor
@Slf4j
public class ViajeController {

    private final AdministrarViajesCasoUso viajes;
    private final ConsultarReporteDeViajeCasoUso reportes;
    private final ViajeExcelGenerador excel;
    private final ViajePdfGenerador pdf;

    @GetMapping
    public ResponseEntity<ResponseGeneric<ViajeResponse>> listar() {
        return ejecutar(() -> new ResponseGeneric<>(viajes.listar().stream().map(ViajeResponse::de).toList()));
    }

    /** Los viajes en los que llegó este producto: el front los usa para avisar antes de contar el flete dos veces. Lista vacía = no está en ningún viaje. */
    @GetMapping("/por-producto/{productoId}")
    public ResponseEntity<ResponseGeneric<ViajeResponse>> deProducto(@PathVariable int productoId) {
        return ejecutar(() -> new ResponseGeneric<>(viajes.deProducto(productoId).stream().map(ViajeResponse::de).toList()));
    }

    /**
     * Descarga el reporte del viaje en Excel o PDF ({@code formato=XLSX|PDF}, por defecto XLSX): bytes con su Content-Type y {@code Content-Disposition: attachment}.
     * Se arma al momento (un viaje es chico). 400 si el formato no es válido o el viaje ya no existe.
     */
    @GetMapping("/{id}/reporte/archivo")
    public ResponseEntity<byte[]> archivo(@PathVariable int id, @RequestParam(defaultValue = "XLSX") String formato) {
        String f = formato == null ? "XLSX" : formato.trim().toUpperCase();
        if (!f.equals("XLSX") && !f.equals("PDF")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            var r = reportes.reporte(id);
            String quien = com.ventas.key.mis.productos.Utils.AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse("admin");
            boolean esPdf = f.equals("PDF");
            byte[] bytes = esPdf ? pdf.generar(r, "Novedades Jade", quien) : excel.generar(r, "Novedades Jade", quien);
            String nombre = "reporte-viaje-" + id + (esPdf ? ".pdf" : ".xlsx");
            return ResponseEntity.ok()
                    .contentType(esPdf ? org.springframework.http.MediaType.APPLICATION_PDF
                            : org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment()
                            .filename(nombre, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                    .body(bytes);
        } catch (ViajeInvalidoException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al armar el archivo del viaje", e);
            return ResponseEntity.status(500).build();
        }
    }

    /** El reporte de todos los viajes (la lista de la pantalla Fletes con su barra de «recuperado»). La lista va en {@code lista}. */
    @GetMapping("/reportes")
    public ResponseEntity<ResponseGeneric<ReporteDeViajeResponse>> reportes() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(reportes.todos().stream().map(ReporteDeViajeResponse::de).toList()));
        } catch (RuntimeException e) {
            log.error("Error en los reportes de viajes", e);
            ResponseGeneric<ReporteDeViajeResponse> r = new ResponseGeneric<>((ReporteDeViajeResponse) null);
            r.setCode(500);
            r.setMensaje("No se pudieron armar los reportes de los viajes.");
            return ResponseEntity.status(500).body(r);
        }
    }

    /** Cómo va el viaje: flete recuperado, lo que falta, desde qué venta se pagó, lo vendido y lo que queda. Solo lectura. */
    @GetMapping("/{id}/reporte")
    public ResponseEntity<ResponseGeneric<ReporteDeViajeResponse>> reporte(@PathVariable int id) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ReporteDeViajeResponse.de(reportes.reporte(id))));
        } catch (ViajeInvalidoException e) {
            ResponseGeneric<ReporteDeViajeResponse> r = new ResponseGeneric<>((ReporteDeViajeResponse) null);
            r.setCode(400);
            r.setMensaje(e.getMessage());
            return ResponseEntity.badRequest().body(r);
        } catch (RuntimeException e) {
            log.error("Error en el reporte del viaje", e);
            ResponseGeneric<ReporteDeViajeResponse> r = new ResponseGeneric<>((ReporteDeViajeResponse) null);
            r.setCode(500);
            r.setMensaje("No se pudo armar el reporte del viaje.");
            return ResponseEntity.status(500).body(r);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseGeneric<ViajeResponse>> obtener(@PathVariable int id) {
        return uno(() -> viajes.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<ViajeResponse>> crear(@RequestBody ViajeRequest request) {
        return uno(() -> viajes.crear(request.aDominio()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<ViajeResponse>> actualizar(@PathVariable int id, @RequestBody ViajeRequest request) {
        return uno(() -> viajes.actualizar(id, request.aDominio()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<ViajeResponse>> eliminar(@PathVariable int id) {
        return ejecutar(() -> {
            viajes.eliminar(id);
            ResponseGeneric<ViajeResponse> r = new ResponseGeneric<>(List.<ViajeResponse>of());
            r.setCode(200);
            r.setMensaje("Viaje eliminado");
            return r;
        });
    }

    private ResponseEntity<ResponseGeneric<ViajeResponse>> uno(Supplier<ViajeDeCompra> accion) {
        return ejecutar(() -> new ResponseGeneric<>(ViajeResponse.de(accion.get())));
    }

    private ResponseEntity<ResponseGeneric<ViajeResponse>> ejecutar(Supplier<ResponseGeneric<ViajeResponse>> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (ViajeInvalidoException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Viaje rechazado por la base: {}", e.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, "No se pudo guardar el viaje: revisa que los productos no estén repetidos y que los textos no sean demasiado largos.");
        } catch (RuntimeException e) {
            log.error("Error en fletes", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación con el viaje.");
        }
    }

    private static ResponseEntity<ResponseGeneric<ViajeResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<ViajeResponse> r = new ResponseGeneric<>((ViajeResponse) null);
        r.setCode(estado.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(r);
    }
}
