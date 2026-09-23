package com.ventas.key.hexagonal.rifa.dominio.excepcion;

/**
 * Se quiso dar de alta un perfil sin ninguna URL de participacion.
 *
 * <p>Un perfil sin participaciones no es un boleto: es una cabecera vacia. La misma idea
 * que en el alta de articulos — si no describe nada, no se guarda.</p>
 *
 * <p>[Hexagonal: Domain Exception] [Clean: Entity]</p>
 */
public class CargaSinParticipacionesException extends CargaBoletosException {

    public CargaSinParticipacionesException() {
        super("No hay ninguna url de participacion que cargar: un perfil sin participaciones no "
                + "suma boletos. Hay que agregar al menos una url de algo que el cliente hizo");
    }
}
