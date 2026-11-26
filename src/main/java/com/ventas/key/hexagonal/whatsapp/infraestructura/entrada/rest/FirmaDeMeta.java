package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Valida la firma {@code X-Hub-Signature-256} con que Meta firma cada petición (HMAC-SHA256 del
 * cuerpo EXACTO con el App Secret). Sin secreto configurado, todo se rechaza.
 */
final class FirmaDeMeta {

    private FirmaDeMeta() {
    }

    static boolean valida(String appSecret, byte[] cuerpo, String cabecera) {
        if (appSecret == null || appSecret.isBlank() || cuerpo == null) {
            return false;
        }
        if (cabecera == null || !cabecera.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String calculada = "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo));
            // Comparación de tiempo constante: no filtra cuántos caracteres coinciden.
            return MessageDigest.isEqual(calculada.getBytes(StandardCharsets.UTF_8),
                    cabecera.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
