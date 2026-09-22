package com.ventas.key.hexagonal.rifa.infraestructura.dto;

import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;

import java.time.LocalDate;
import java.util.List;

/**
 * Un renglon de la pantalla: el perfil con sus participaciones adentro y cuantos boletos
 * suma (regla R4).
 *
 * <p>[Hexagonal: Adapter DTO] [Clean: Interface Adapter]</p>
 */
public record GrupoBoletosResponse(Integer concursanteId, String nombreConcursante,
        String plataforma, String urlPerfil, int totalBoletos, LocalDate ultimaParticipacion,
        List<ParticipacionResponse> participaciones) {

    public static GrupoBoletosResponse de(GrupoDeBoletos grupo) {
        return new GrupoBoletosResponse(grupo.concursanteId(), grupo.nombreConcursante(),
                grupo.perfil().plataforma().name(), grupo.perfil().urlPerfil(),
                grupo.totalBoletos(), grupo.ultimaParticipacion(),
                grupo.participaciones().stream().map(ParticipacionResponse::de).toList());
    }

    /** {@code boletoId} es lo que hay que mandar para quitar esta participacion. */
    public record ParticipacionResponse(Integer boletoId, String urlParticipacion, String motivo,
            LocalDate fecha) {

        public static ParticipacionResponse de(
                com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion p) {
            return new ParticipacionResponse(p.boletoId(), p.urlParticipacion(), p.motivo(),
                    p.fecha());
        }
    }
}
