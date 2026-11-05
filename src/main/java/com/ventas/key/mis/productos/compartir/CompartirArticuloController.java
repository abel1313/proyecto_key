package com.ventas.key.mis.productos.compartir;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.VarianteServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Vista previa para compartir un articulo (ver CAMBIOS_FRONT.md). Publica, solo lectura. */
@Slf4j
@RestController
@RequestMapping("/v1/compartir")
public class CompartirArticuloController {

    private static final String HTML_404 = "<!doctype html><meta charset=\"utf-8\"><meta name=\"robots\" content=\"noindex\"><title>No encontrado</title>";

    private final IVarianteRepository variantes;
    private final VarianteServiceImpl servicio;
    private final String baseBack;
    private final String baseTienda;

    public CompartirArticuloController(IVarianteRepository variantes, VarianteServiceImpl servicio,
                                       @Value("${app.public-base-url:}") String baseBack,
                                       @Value("${app.tienda-base-url:}") String baseTienda) {
        this.variantes = variantes;
        this.servicio = servicio;
        this.baseBack = baseHttpSegura(baseBack, "app.public-base-url");
        this.baseTienda = baseHttpSegura(baseTienda, "app.tienda-base-url");
    }

    /** String y no Integer: "abc", "-1" o "99999999999" responden el mismo 404 HTML en vez de un 400 JSON del ExceptionGlobal. */
    @GetMapping(value = "/articulo/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> articulo(@PathVariable String id) {
        Integer vid = idPositivo(id);
        Variantes v = vid == null ? null : variantes.findById(vid).orElse(null);
        Producto p = v == null ? null : v.getProducto();
        if (v == null || p == null || v.getHabilitado() != '1' || p.getHabilitado() != '1'
                || Boolean.TRUE.equals(p.getEsCatalogoInterno())) {   // productos "sombra": nunca en el catalogo publico
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noStore()).body(HTML_404);
        }
        String descripcion = v.getDescripcion() != null && !v.getDescripcion().isBlank() ? v.getDescripcion() : p.getDescripcion();
        Double normal = v.precioNormal();
        String precio = normal == null || normal.isNaN() || normal.isInfinite() || normal <= 0 ? "" : String.format(Locale.ROOT, "%.2f", normal);
        String urlTienda = baseTienda.isEmpty() ? "" : baseTienda + "/tienda/detalle/" + vid;
        String canonica = baseBack.isEmpty() ? "" : baseBack + "/v1/compartir/articulo/" + vid;
        String html = VistaPreviaArticuloHtml.armar(p.getNombre(), descripcion, precio, imagen(vid), urlTienda, canonica);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .contentType(MediaType.TEXT_HTML).body(html);
    }

    private String imagen(Integer vid) {
        if (baseBack.isEmpty()) return "";
        try {
            List<ImagenUpdateDto> imgs = servicio.getImagenesPorVarianteV2(vid);
            if (imgs == null || imgs.isEmpty()) return "";
            ImagenUpdateDto principal = imgs.stream().filter(i -> Boolean.TRUE.equals(i.getPrincipal())).findFirst().orElse(imgs.get(0));
            return principal.getId() == null ? "" : baseBack + "/v1/imagenes/file/" + principal.getId();
        } catch (RuntimeException e) {
            log.warn("compartir: sin foto para articuloId={}: {}", vid, e.toString());
            return "";
        }
    }

    static Integer idPositivo(String s) {
        if (s == null || s.length() > 10 || !s.chars().allMatch(Character::isDigit) || s.isEmpty()) return null;
        long n = Long.parseLong(s);
        return n >= 1 && n <= Integer.MAX_VALUE ? (int) n : null;
    }

    /** Solo http/https absolutas, sin comillas ni espacios; cualquier otra cosa se ignora (falla segura). */
    static String baseHttpSegura(String s, String nombre) {
        if (s == null || s.isBlank()) return "";
        String x = s.strip();
        while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
        try {
            URI u = URI.create(x);
            String esq = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
            if ((esq.equals("https") || esq.equals("http")) && u.getHost() != null && u.getQuery() == null && u.getFragment() == null) return x;
        } catch (IllegalArgumentException ignored) { }
        log.error("{} no es una URL http(s) valida; se ignora", nombre);
        return "";
    }
}
