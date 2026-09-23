package com.ventas.key.hexagonal.rifa.infraestructura.dto;

import com.ventas.key.hexagonal.rifa.dominio.modelo.ModoDeCarga;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;

import java.util.List;

/**
 * Alta de un perfil con varias participaciones de una sola pasada.
 *
 * <p>La cabecera (concursante, plataforma, perfil) viaja una vez; cada elemento de
 * {@code participaciones} se convierte en un boleto.</p>
 *
 * <p>[Hexagonal: Adapter DTO] [Clean: Interface Adapter]</p>
 */
public class CargarBoletosRequest {

    private Integer concursanteId;
    private Plataforma plataforma;
    private String urlPerfil;
    private List<ParticipacionRequest> participaciones;

    public Integer getConcursanteId() {
        return concursanteId;
    }

    public void setConcursanteId(Integer concursanteId) {
        this.concursanteId = concursanteId;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    public String getUrlPerfil() {
        return urlPerfil;
    }

    public void setUrlPerfil(String urlPerfil) {
        this.urlPerfil = urlPerfil;
    }

    public List<ParticipacionRequest> getParticipaciones() {
        return participaciones;
    }

    public void setParticipaciones(List<ParticipacionRequest> participaciones) {
        this.participaciones = participaciones;
    }

    /**
     * Una URL de participacion.
     *
     * <p>{@code modo} decide como se valida (regla D2): {@code UNICA} la rechaza si ya
     * existe en la rifa, {@code REPETIDA_PERMITIDA} la acepta igual. Si no viene, se toma
     * como {@code UNICA}.</p>
     */
    public static class ParticipacionRequest {

        private String urlParticipacion;
        private String motivo;
        private ModoDeCarga modo;

        public String getUrlParticipacion() {
            return urlParticipacion;
        }

        public void setUrlParticipacion(String urlParticipacion) {
            this.urlParticipacion = urlParticipacion;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }

        public ModoDeCarga getModo() {
            return modo;
        }

        public void setModo(ModoDeCarga modo) {
            this.modo = modo;
        }
    }
}
