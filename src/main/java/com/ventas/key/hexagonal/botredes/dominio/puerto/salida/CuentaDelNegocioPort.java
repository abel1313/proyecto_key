package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;

/**
 * Reconoce a la propia cuenta del negocio (página de Facebook o cuenta de Instagram), para que el
 * bot nunca se conteste a sí mismo.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface CuentaDelNegocioPort {

    boolean esDelNegocio(RedSocial red, String autorId);
}
