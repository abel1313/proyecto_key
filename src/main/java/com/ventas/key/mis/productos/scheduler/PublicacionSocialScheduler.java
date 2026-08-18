package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.redessociales.IPublicacionSocialRepository;
import com.ventas.key.mis.productos.redessociales.PublicacionSocial;
import com.ventas.key.mis.productos.redessociales.PublicacionSocialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// Dispara las publicaciones "PROGRAMADA" (Facebook, Instagram y TikTok por igual -- decision del
// dueno de no usar el scheduler nativo de Meta, ver PublicacionSocialService) cuando llega su
// scheduledPublishTime. Corre cada minuto: suficiente margen para que ninguna publicacion salga
// tarde de forma notoria, sin sobrecargar la Graph API con consultas constantes.
@Component
@Slf4j
public class PublicacionSocialScheduler {

    private final IPublicacionSocialRepository publicacionSocialRepository;
    private final PublicacionSocialService publicacionSocialService;

    public PublicacionSocialScheduler(IPublicacionSocialRepository publicacionSocialRepository,
                                       PublicacionSocialService publicacionSocialService) {
        this.publicacionSocialRepository = publicacionSocialRepository;
        this.publicacionSocialService = publicacionSocialService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void publicarProgramadas() {
        List<PublicacionSocial> pendientes = publicacionSocialRepository
                .findByEstadoAndScheduledPublishTimeLessThanEqual("PROGRAMADA", LocalDateTime.now());

        if (pendientes.isEmpty()) {
            return;
        }
        log.info("Publicaciones programadas por disparar: {}", pendientes.size());
        for (PublicacionSocial p : pendientes) {
            publicacionSocialService.ejecutarProgramada(p);
        }
    }
}
