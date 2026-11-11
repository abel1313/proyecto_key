package com.ventas.key.hexagonal.reportecategoria.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.reportecategoria.aplicacion.servicio.ArmarReporteService;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReporteCalculado;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TipoReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.VistaReporte;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.config.ProcesadorReporteAsincrono;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.dto.FilaReporteResponse;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.dto.GenerarReporteRequest;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.dto.PaginaReporteResponse;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.dto.ReporteGeneradoResponse;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.ReporteGeneradoEntidad;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.repository.IReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: Driving Adapter] Reporte por categoría / por productos elegidos. HECHO EN DURO solo para ROLE_ADMIN
 * (SecurityConfig: /v1/reportes/ventas/por-categoria/**); pendiente pasarlo a una pantalla/acción configurable del menú.
 * Contrato en CAMBIOS_FRONT.md.
 */
@RestController
@RequestMapping("/v1/reportes/ventas/por-categoria")
@RequiredArgsConstructor
public class ReporteCategoriaController {

    private static final int MAX_PRODUCTOS_ELEGIDOS = 500;
    private static final int MAX_EN_CURSO = 3;
    private static final int DIAS_VIGENCIA = 7;

    private final ArmarReporteService armar;
    private final ProcesadorReporteAsincrono procesador;
    private final IReporteGeneradoRepository repo;

    /**
     * El reporte en pantalla, paginado (solo lectura: no guarda nada). Sin {@code categoria} ni {@code sinCategoria} trae todo.
     * {@code productoIds} = ids separados por coma. Página desde 1.
     */
    @GetMapping
    public ResponseEntity<ResponseGeneric<PaginaReporteResponse>> ver(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean sinCategoria,
            @RequestParam(required = false) List<Integer> productoIds,
            @RequestParam(defaultValue = "TODOS") String vista,
            @RequestParam(defaultValue = "COMPLETO") String tipoReporte,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        FiltroReporte filtro = filtro(desde, hasta, categoria, sinCategoria, productoIds, vista, tipoReporte);
        ReporteCalculado r = armar.armar(filtro, false, null);
        int tam = Math.max(1, Math.min(size, 100));
        int total = r.filas().size();
        int paginas = Math.max(1, (int) Math.ceil(total / (double) tam));
        int p = Math.max(1, Math.min(pagina, paginas));
        List<FilaReporteResponse> filas = r.filas().stream().skip((long) (p - 1) * tam).limit(tam).map(FilaReporteResponse::de).toList();
        return ResponseEntity.ok(new ResponseGeneric<>(new PaginaReporteResponse(
                descripcionDe(filtro), filas, p, paginas, total, r.totales())));
    }

    /** Pide el reporte en Excel o PDF: se arma en segundo plano y responde al instante con su id (estado PENDIENTE). */
    @PostMapping("/generar")
    public ResponseEntity<ResponseGeneric<ReporteGeneradoResponse>> generar(@RequestBody GenerarReporteRequest req) {
        if (req == null) throw new IllegalArgumentException("Falta lo que se pide.");
        FiltroReporte filtro = filtro(req.desde(), req.hasta(), req.categoria(), Boolean.TRUE.equals(req.sinCategoria()), req.productoIds(), req.vista(), req.tipoReporte());
        String formato = req.formato() == null || req.formato().isBlank() ? "XLSX" : req.formato().trim().toUpperCase();
        if (!formato.equals("XLSX") && !formato.equals("PDF")) throw new IllegalArgumentException("El formato debe ser XLSX o PDF.");
        String usuario = AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse("admin");

        if (repo.countByUsuarioAndEstadoIn(usuario, List.of("PENDIENTE", "GENERANDO")) >= MAX_EN_CURSO) {
            throw new IllegalStateException("Ya tienes " + MAX_EN_CURSO + " reportes armándose. Espera a que termine alguno.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        ReporteGeneradoEntidad e = new ReporteGeneradoEntidad();
        e.setUsuario(usuario);
        e.setEstado("PENDIENTE");
        e.setFormato(formato);
        e.setDescripcion(descripcionDe(filtro));
        e.setCreado(ahora);
        e.setExpira(ahora.plusDays(DIAS_VIGENCIA));
        e = repo.save(e);

        String nombre = (filtro.tipo() == TipoReporte.VENTAS ? "reporte-ventas-por-tipo-de-precio-" : "reporte-por-categoria-") + filtro.desde() + "_" + filtro.hasta() + (formato.equals("PDF") ? ".pdf" : ".xlsx");
        procesador.procesar(e.getId(), filtro, formato, usuario, nombre);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseGeneric<>(
                new ReporteGeneradoResponse(e.getId(), e.getEstado(), formato, e.getDescripcion(), null, null, e.getCreado(), null, e.getExpira(), 0),
                "El reporte se está armando. Avisa cuando esté listo."));
    }

    /** Mis reportes pedidos, del más nuevo al más viejo (sin el archivo). La lista va en {@code lista} (convención de ResponseGeneric). */
    @GetMapping("/generados")
    public ResponseEntity<ResponseGeneric<ReporteGeneradoResponse>> generados(@RequestParam(defaultValue = "20") int max) {
        String usuario = AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse("admin");
        List<ReporteGeneradoResponse> lista = repo.resumenesDe(usuario, PageRequest.of(0, Math.max(1, Math.min(max, 50))))
                .stream().map(ReporteGeneradoResponse::deFila).toList();
        return ResponseEntity.ok(new ResponseGeneric<>(lista, "Reportes generados"));
    }

    /** El estado de un reporte (para preguntar cada tanto si ya está LISTO). 404 si no es mío. */
    @GetMapping("/generados/{id}")
    public ResponseEntity<ResponseGeneric<ReporteGeneradoResponse>> estado(@PathVariable int id) {
        String usuario = AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse("admin");
        return repo.resumenesDe(usuario, PageRequest.of(0, 50)).stream()
                .filter(f -> ((Number) f[0]).intValue() == id).findFirst()
                .map(f -> ResponseEntity.ok(new ResponseGeneric<>(ReporteGeneradoResponse.deFila(f))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Descarga el archivo (bytes; Content-Type de Excel o PDF). 404 si no existe, no es mío o aún no está LISTO. */
    @GetMapping("/generados/{id}/archivo")
    public ResponseEntity<byte[]> archivo(@PathVariable int id) {
        String usuario = AuthenticationUtils.currentUsuarioOpt().map(u -> u.getUsername()).orElse("admin");
        return repo.findById(id)
                .filter(r -> usuario.equals(r.getUsuario()) && "LISTO".equals(r.getEstado()) && r.getContenido() != null)
                .map(r -> ResponseEntity.ok()
                        .contentType("PDF".equals(r.getFormato()) ? MediaType.APPLICATION_PDF
                                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(r.getNombreArchivo() == null ? "reporte" : r.getNombreArchivo(), StandardCharsets.UTF_8).build().toString())
                        .body(r.getContenido()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseGeneric<Object>> datosNoValidos(IllegalArgumentException e) {
        ResponseGeneric<Object> r = new ResponseGeneric<>((Object) null);
        r.setMensaje(e.getMessage());
        return ResponseEntity.badRequest().body(r);
    }

    @ExceptionHandler({IllegalStateException.class, TaskRejectedException.class})
    public ResponseEntity<ResponseGeneric<Object>> demasiados(RuntimeException e) {
        ResponseGeneric<Object> r = new ResponseGeneric<>((Object) null);
        r.setMensaje(e instanceof TaskRejectedException ? "Hay muchos reportes en cola. Intenta en un momento." : e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(r);
    }

    private static FiltroReporte filtro(LocalDate desde, LocalDate hasta, String categoria, boolean sinCategoria, List<Integer> ids, String vista, String tipo) {
        if (ids != null && ids.size() > MAX_PRODUCTOS_ELEGIDOS) throw new IllegalArgumentException("Elige máximo " + MAX_PRODUCTOS_ELEGIDOS + " productos.");
        return new FiltroReporte(desde, hasta, categoria, sinCategoria, ids, VistaReporte.de(vista), TipoReporte.de(tipo));
    }

    private static String descripcionDe(FiltroReporte f) {
        String que = f.porProductos() ? f.productoIds().size() + " productos elegidos"
                : f.categoria() != null ? "Categoría " + f.categoria() : f.sinCategoria() ? "Otros (sin categoría)" : "Todas las categorías";
        return que + " · " + f.desde() + " a " + f.hasta() + " · " + f.vista() + (f.tipo() == TipoReporte.VENTAS ? " · Ventas por tipo de precio" : "");
    }
}
