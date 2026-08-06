package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.redessociales.PublicacionSocialDto;
import com.ventas.key.mis.productos.redessociales.PublicacionSocialService;
import com.ventas.key.mis.productos.redessociales.PublicarFacebookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Tag(name = "Redes sociales", description = "Publicar variantes del catálogo en Facebook")
@RestController
@RequestMapping("/v1/redes-sociales")
@RequiredArgsConstructor
@Slf4j
public class RedesSocialesController {

    private final PublicacionSocialService publicacionSocialService;

    @Operation(
        summary = "Publicar una variante en la página de Facebook",
        description = "Sube una foto junto con la descripción a POST /{page-id}/photos de la Graph API. " +
                "La imagen puede ser: (1) la principal ya guardada de la variante (default, sin mandar nada), " +
                "(2) otra imagen ya guardada de esa variante (mandando imagenId), o (3) un archivo nuevo " +
                "(imagenNueva) que se publica tal cual llega -- a máxima calidad, sin redimensionar -- y que " +
                "NO se guarda en la galería de la variante, es exclusivo de esa publicación. " +
                "Si scheduledPublishTime viene, programa la publicación (published=false); si no, publica de inmediato."
    )
    @PostMapping(value = "/facebook/publicar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarEnFacebook(
            @RequestParam Integer varianteId,
            @RequestParam String descripcion,
            @RequestParam(required = false) Long imagenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledPublishTime,
            @RequestParam(required = false) MultipartFile imagenNueva) {

        log.info("Publicar en Facebook - varianteId={}, imagenId={}, imagenNueva={}",
                varianteId, imagenId, imagenNueva != null && !imagenNueva.isEmpty());

        PublicarFacebookRequest request = new PublicarFacebookRequest();
        request.setVarianteId(varianteId);
        request.setDescripcion(descripcion);
        request.setImagenId(imagenId);
        request.setScheduledPublishTime(scheduledPublishTime);

        PublicacionSocialDto publicacion = publicacionSocialService.publicarEnFacebook(request, imagenNueva);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }
}
