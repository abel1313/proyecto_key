package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Valida que un aviso (webhook) viene de Mercado Pago y no de cualquiera que conozca la URL.
 *
 * <p>Mercado Pago manda el encabezado {@code x-signature: ts=<marca>,v1=<firma>} y {@code x-request-id}. La firma es
 * HMAC-SHA256 (hex) con el secreto del webhook sobre el texto {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}
 * donde {@code data.id} sale de la URL de la notificacion (en minusculas si es alfanumerico). Se compara en tiempo
 * constante. Sin secreto configurado NUNCA se da por valido un aviso.
 */
public final class FirmaWebhookMercadoPago {

    private final String secreto;

    public FirmaWebhookMercadoPago(String secreto) {
        this.secreto = secreto == null ? "" : secreto.trim();
    }

    public boolean esValida(String encabezadoFirma, String requestId, String dataId) {
        if (secreto.isEmpty() || encabezadoFirma == null || dataId == null || dataId.isBlank()) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : encabezadoFirma.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].trim().equals("ts")) ts = kv[1].trim();
            if (kv[0].trim().equals("v1")) v1 = kv[1].trim();
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifiesto = "id:" + dataId.toLowerCase(Locale.ROOT) + ";"
                + (requestId != null && !requestId.isBlank() ? "request-id:" + requestId + ";" : "")
                + "ts:" + ts + ";";
        byte[] esperada = hmac(manifiesto).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperada, v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la firma", e);
        }
    }
}
