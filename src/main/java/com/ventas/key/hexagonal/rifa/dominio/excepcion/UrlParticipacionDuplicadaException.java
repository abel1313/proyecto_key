package com.ventas.key.hexagonal.rifa.dominio.excepcion;

/**
 * La URL de participacion se cargo en modo {@code UNICA} y ese mismo perfil ya la tenia en
 * esta rifa (regla D2).
 *
 * <p>Pasa cuando la persona participo dos veces en la misma publicacion (por ejemplo,
 * compartio y ademas comento). Si es asi, se vuelve a cargar en modo
 * {@code REPETIDA_PERMITIDA}; el front lo pregunta antes de reenviarla.</p>
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
                "%s ya tiene cargada esta publicación con este mismo perfil (%s). Si participó "
                        + "otra vez en ella (por ejemplo, compartió y además comentó), se puede "
                        + "cargar como que se repite.",
                nombreDelDueno, urlParticipacion));
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
