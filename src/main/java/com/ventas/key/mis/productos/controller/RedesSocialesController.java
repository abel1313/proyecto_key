package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.redessociales.ActualizarHashtagsRequest;
import com.ventas.key.mis.productos.redessociales.AutorizarTikTokRequest;
import com.ventas.key.mis.productos.redessociales.HashtagsDefault;
import com.ventas.key.mis.productos.redessociales.PublicacionSocialDto;
import com.ventas.key.mis.productos.redessociales.PublicacionSocialService;
import com.ventas.key.mis.productos.redessociales.PublicarFacebookRequest;
import com.ventas.key.mis.productos.redessociales.PublicarInstagramRequest;
import com.ventas.key.mis.productos.redessociales.TikTokGraphClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Tag(name = "Redes sociales", description = "Publicar variantes del catálogo en Facebook, Instagram y TikTok")
@RestController
@RequestMapping("/v1/redes-sociales")
@RequiredArgsConstructor
@Slf4j
public class RedesSocialesController {

    private final PublicacionSocialService publicacionSocialService;
    private final TikTokGraphClient tikTokGraphClient;

    @Operation(
        summary = "Publicar una variante en la página de Facebook",
        description = "Sube una foto junto con la descripción a POST /{page-id}/photos de la Graph API. " +
                "La imagen puede ser: (1) la principal ya guardada de la variante (default, sin mandar nada), " +
                "(2) otra imagen ya guardada de esa variante (mandando imagenId), o (3) un archivo nuevo " +
                "(imagenNueva) que se publica tal cual llega -- a máxima calidad, sin redimensionar -- y que " +
                "NO se guarda en la galería de la variante, es exclusivo de esa publicación. " +
                "Si scheduledPublishTime viene, se guarda como PROGRAMADA y la publica nuestro propio job a esa " +
                "hora (no el scheduler nativo de Meta -- decisión explícita para que Facebook/Instagram/TikTok " +
                "salgan siempre sincronizados); si no, publica de inmediato."
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

    @Operation(
        summary = "Publicar un video de una variante en la página de Facebook",
        description = "Sube un video junto con la descripción a POST /{page-id}/videos de la Graph API. " +
                "A diferencia de la foto, el catálogo no guarda video de variantes -- el archivo es " +
                "obligatorio en cada llamada, nunca se persiste en el microservicio de imágenes. " +
                "Si scheduledPublishTime viene, se guarda como PROGRAMADA y la publica nuestro propio job a esa " +
                "hora; si no, publica de inmediato."
    )
    @PostMapping(value = "/facebook/publicar-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarVideoEnFacebook(
            @RequestParam Integer varianteId,
            @RequestParam String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledPublishTime,
            @RequestParam MultipartFile video) {

        log.info("Publicar video en Facebook - varianteId={}, bytes={}", varianteId, video.getSize());

        PublicacionSocialDto publicacion = publicacionSocialService.publicarVideoEnFacebook(
                varianteId, descripcion, scheduledPublishTime, video);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }

    @Operation(
        summary = "Publicar un Reel de una variante en la página de Facebook",
        description = "Sube el video con subida reanudable (POST /{page-id}/video_reels, varios pasos absorbidos " +
                "aqui) y publica un Reel. Sin validar duracion/proporcion/peso -- lo que Facebook rechace lo " +
                "rechaza con su propio error. Si scheduledPublishTime viene, se guarda como PROGRAMADA y la " +
                "publica nuestro propio job a esa hora (no el scheduler nativo de Meta, por consistencia con " +
                "Instagram/TikTok que no lo tienen); si no, publica de inmediato. El archivo es obligatorio en " +
                "cada llamada, nunca se persiste."
    )
    @PostMapping(value = "/facebook/publicar-reel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarReelEnFacebook(
            @RequestParam Integer varianteId,
            @RequestParam String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledPublishTime,
            @RequestParam MultipartFile video) {

        log.info("Publicar Reel en Facebook - varianteId={}, bytes={}", varianteId, video.getSize());

        PublicacionSocialDto publicacion = publicacionSocialService.publicarReelEnFacebook(
                varianteId, descripcion, scheduledPublishTime, video);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }

    @Operation(
        summary = "Publicar una variante en la cuenta de Instagram vinculada a la página",
        description = "Primera versión: solo foto ya guardada en el catálogo (imagenId o la principal de la " +
                "variante si se omite) -- Instagram necesita una URL pública de la imagen, no acepta un archivo " +
                "nuevo subido directo como Facebook. Si scheduledPublishTime viene, se guarda como PROGRAMADA y " +
                "la publica nuestro propio job (Instagram no tiene programación nativa, es un límite real de su " +
                "API); si no, publica de inmediato. Requiere que la cuenta de Instagram ya esté vinculada a la " +
                "página en Meta Business Suite."
    )
    @PostMapping("/instagram/publicar")
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarEnInstagram(
            @RequestBody PublicarInstagramRequest request) {

        log.info("Publicar en Instagram - varianteId={}, imagenId={}", request.getVarianteId(), request.getImagenId());

        PublicacionSocialDto publicacion = publicacionSocialService.publicarEnInstagram(request);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }

    @Operation(
        summary = "Completar el login OAuth de TikTok (paso único, manual)",
        description = "Cambia el 'code' que TikTok manda por URL tras autorizar la cuenta por el primer " +
                "access_token/refresh_token, y los guarda -- de ahí en adelante se refrescan solos. Ver " +
                "TIKTOK_SETUP.md paso 4-5. Solo hace falta llamarlo una vez, salvo que se revoque el acceso."
    )
    @PostMapping("/tiktok/autorizar")
    public ResponseEntity<ResponseGeneric<String>> autorizarTikTok(@RequestBody AutorizarTikTokRequest request) {
        try {
            tikTokGraphClient.autorizarConCode(request.getCode(), request.getRedirectUri());
            return ResponseEntity.ok(new ResponseGeneric<>("TikTok autorizado correctamente"));
        } catch (Exception e) {
            log.warn("Error al autorizar TikTok: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>((String) null, e.getMessage()));
        }
    }

    @Operation(
        summary = "Subir un video de una variante a TikTok (modo Upload/borrador)",
        description = "Sube el video con el modo 'Upload' de la Content Posting API v2 -- llega como borrador " +
                "al inbox de la cuenta autorizada, el dueño lo tiene que terminar de publicar manualmente desde " +
                "el celular. No es Direct Post: el scope video.publish que se necesitaría para auto-publicar no " +
                "está disponible sin pasar la auditoría de TikTok. Sin validar duración/proporción/peso -- lo " +
                "que TikTok rechace lo rechaza con su propio error. Si scheduledPublishTime viene, se guarda " +
                "como PROGRAMADA y la publica (sube el borrador) nuestro propio job a esa hora; si no, sube de " +
                "inmediato. El archivo es obligatorio en cada llamada, nunca se persiste."
    )
    @PostMapping(value = "/tiktok/publicar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarEnTikTok(
            @RequestParam Integer varianteId,
            @RequestParam String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledPublishTime,
            @RequestParam MultipartFile video) {

        log.info("Publicar en TikTok - varianteId={}, bytes={}", varianteId, video.getSize());

        PublicacionSocialDto publicacion = publicacionSocialService.publicarEnTikTok(
                varianteId, descripcion, scheduledPublishTime, video);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }

    @Operation(
        summary = "Publicar un Reel de una variante en Instagram",
        description = "Sube el video con subida reanudable (varios pasos con la Graph API, absorbidos aqui) y " +
                "publica un Reel. El video se manda tal cual llega -- sin validar duracion/proporcion/peso, " +
                "vertical u horizontal, lo que Instagram rechace lo rechaza con su propio error. Si " +
                "scheduledPublishTime viene, se guarda como PROGRAMADA y la publica nuestro propio job " +
                "(Instagram no tiene programación nativa); si no, publica de inmediato. El archivo es " +
                "obligatorio en cada llamada, nunca se persiste."
    )
    @PostMapping(value = "/instagram/publicar-reel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<PublicacionSocialDto>> publicarReelEnInstagram(
            @RequestParam Integer varianteId,
            @RequestParam String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledPublishTime,
            @RequestParam MultipartFile video) {

        log.info("Publicar Reel en Instagram - varianteId={}, bytes={}", varianteId, video.getSize());

        PublicacionSocialDto publicacion = publicacionSocialService.publicarReelEnInstagram(
                varianteId, descripcion, scheduledPublishTime, video);
        return ResponseEntity.ok(new ResponseGeneric<>(publicacion));
    }

    @Operation(
        summary = "Obtener los hashtags por default de las 3 redes",
        description = "Un set fijo de hashtags por red (facebook/instagram/tiktok) para que el front los " +
                "precargue siempre al abrir el formulario de publicar -- el admin ya no los reescribe a mano " +
                "cada vez, aunque puede editarlos/quitarlos antes de enviar. Devuelve las 3 filas siempre " +
                "(se siembran vacías en la migración)."
    )
    @GetMapping("/hashtags-default")
    public ResponseEntity<ResponseGeneric<HashtagsDefault>> obtenerHashtagsDefault() {
        return ResponseEntity.ok(new ResponseGeneric<HashtagsDefault>(publicacionSocialService.obtenerHashtagsDefault()));
    }

    @Operation(
        summary = "Actualizar los hashtags por default de una red",
        description = "redSocial debe ser 'facebook', 'instagram' o 'tiktok'. Reemplaza el valor completo " +
                "guardado para esa red -- no es un append."
    )
    @PutMapping("/hashtags-default/{redSocial}")
    public ResponseEntity<ResponseGeneric<HashtagsDefault>> actualizarHashtagsDefault(
            @PathVariable String redSocial, @RequestBody ActualizarHashtagsRequest request) {

        HashtagsDefault actualizado = publicacionSocialService.actualizarHashtagsDefault(redSocial, request.getHashtags());
        return ResponseEntity.ok(new ResponseGeneric<>(actualizado));
    }
}
