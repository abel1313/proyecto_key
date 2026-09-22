package com.ventas.key.hexagonal.rifa.dominio.excepcion;

/**
 * La URL de participacion se cargo en modo {@code UNICA} y ya existia en esta rifa
 * (regla D2).
 *
 * <p>No alcanza con decir "duplicada": el mensaje dice <b>de quien</b> es la que ya estaba,
 * porque lo primero que hace el admin es querer saber si se la esta robando a otro cliente
 * o si es suya de antes. Si de verdad se repite, se vuelve a cargar en modo
 * {@code REPETIDA_PERMITIDA}.</p>
 *
 * <p>[Hexagonal: Domain Exception] [Clean: Entity]</p>
 */
public class UrlParticipacionDuplicadaException extends CargaBoletosException {

    private final String urlParticipacion;
    private final String nombreDelDueno;
    private final Integer boletoExistenteId;

    public UrlParticipacionDuplicadaException(String urlParticipacion, String nombreDelDueno,
            Integer boletoExistenteId) {
        super(String.format(
                "La url '%s' ya esta cargada como boleto de '%s'. Si de verdad se repite, hay que "
                        + "volver a cargarla con modo 'REPETIDA_PERMITIDA'",
                urlParticipacion, nombreDelDueno));
        this.urlParticipacion = urlParticipacion;
        this.nombreDelDueno = nombreDelDueno;
        this.boletoExistenteId = boletoExistenteId;
    }

    public String getUrlParticipacion() {
        return urlParticipacion;
    }

    public String getNombreDelDueno() {
        return nombreDelDueno;
    }

    public Integer getBoletoExistenteId() {
        return boletoExistenteId;
    }
}
