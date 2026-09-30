package com.ventas.key.mis.productos.redessociales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Al arrancar, anota en el log qué variables de entorno de redes sociales faltan o vienen vacías.
 * Nunca escribe los valores, solo los nombres.
 *
 * Por qué existe (2026-09-30): en prod TIKTOK_CLIENT_KEY/SECRET quedaron dadas de alta en el
 * deployment pero VACÍAS. El back arrancó sin quejarse, `printenv | grep -c` decía que "sí estaban"
 * y el error solo apareció al tocar "Conectar TikTok". Con esto, `kubectl logs ... | grep -i redes`
 * lo dice en cuanto arranca el pod.
 */
@Component
@Slf4j
public class RevisionConfigRedesSociales {

    private final Map<String, String> variables = new LinkedHashMap<>();

    public RevisionConfigRedesSociales(
            @Value("${facebook.page-id:}") String facebookPageId,
            @Value("${facebook.page-access-token:}") String facebookPageAccessToken,
            @Value("${facebook.app-secret:}") String facebookAppSecret,
            @Value("${facebook.webhook-verify-token:}") String facebookWebhookVerifyToken,
            @Value("${instagram.account-id:}") String instagramAccountId,
            @Value("${tiktok.client-key:}") String tiktokClientKey,
            @Value("${tiktok.client-secret:}") String tiktokClientSecret) {
        variables.put("FACEBOOK_PAGE_ID", facebookPageId);
        variables.put("FACEBOOK_PAGE_ACCESS_TOKEN", facebookPageAccessToken);
        variables.put("FACEBOOK_APP_SECRET", facebookAppSecret);
        variables.put("FACEBOOK_WEBHOOK_VERIFY_TOKEN", facebookWebhookVerifyToken);
        variables.put("INSTAGRAM_ACCOUNT_ID", instagramAccountId);
        variables.put("TIKTOK_CLIENT_KEY", tiktokClientKey);
        variables.put("TIKTOK_CLIENT_SECRET", tiktokClientSecret);
    }

    /** Nombres de las variables que faltan o vienen vacías (o solo con espacios). */
    List<String> faltantes() {
        List<String> faltan = new ArrayList<>();
        variables.forEach((nombre, valor) -> {
            if (valor == null || valor.isBlank()) {
                faltan.add(nombre);
            }
        });
        return faltan;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void revisar() {
        List<String> faltan = faltantes();
        if (faltan.isEmpty()) {
            log.info("Redes sociales: configuración completa (Facebook, Instagram, TikTok)");
        } else {
            log.warn("Redes sociales: faltan o están VACÍAS estas variables de entorno: {} -- esa red no va a "
                    + "poder publicar/conectar hasta cargarlas y reiniciar el pod (ver TIKTOK_SETUP.md y "
                    + "VPS_AUDITORIA.md)", faltan);
        }
    }
}
