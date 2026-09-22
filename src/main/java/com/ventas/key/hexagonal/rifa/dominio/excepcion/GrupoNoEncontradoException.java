package com.ventas.key.hexagonal.rifa.dominio.excepcion;

/**
 * Se quiso agregar o quitar una participacion de un grupo (plataforma + perfil) que no
 * existe en esta rifa.
 *
 * <p>[Hexagonal: Domain Exception] [Clean: Entity]</p>
 */
public class GrupoNoEncontradoException extends CargaBoletosException {

    public GrupoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
