package com.ventas.key.hexagonal.rifa.dominio.modelo;

/**
 * Como se valida la URL de una participacion al cargarla (regla D2).
 *
 * <p>Se elige por participacion, no una vez para toda la rifa: el caso real es
 * "normalmente quiero que la URL sea unica, pero esta en particular se repite de verdad
 * y necesito poder cargarla igual".</p>
 *
 * <p>[Hexagonal: Domain Model] [Clean: Entity]</p>
 */
public enum ModoDeCarga {

    /** Si la URL ya existe en esta rifa, se rechaza y se dice en que grupo esta. */
    UNICA,

    /** Se acepta aunque la URL ya exista. */
    REPETIDA_PERMITIDA;

    public static ModoDeCarga oPorDefecto(ModoDeCarga modo) {
        return modo != null ? modo : UNICA;
    }

    public boolean exigeQueNoExista() {
        return this == UNICA;
    }
}
