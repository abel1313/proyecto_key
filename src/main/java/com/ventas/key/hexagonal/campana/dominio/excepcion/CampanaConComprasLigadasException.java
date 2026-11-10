package com.ventas.key.hexagonal.campana.dominio.excepcion;

/** La campaña ya tiene compras ligadas: no se borra (se perdería el historial); se apaga. El controller la responde con 409. */
public class CampanaConComprasLigadasException extends CampanaInvalidaException {

    public static final String MENSAJE =
            "Esta campaña ya tiene compras ligadas: apágala en lugar de eliminarla, así se conserva el historial.";

    public CampanaConComprasLigadasException() {
        super(MENSAJE);
    }
}
