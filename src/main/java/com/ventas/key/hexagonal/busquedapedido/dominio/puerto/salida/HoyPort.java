package com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida;

import java.time.LocalDate;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Que dia es hoy en la tienda. El servidor puede estar en UTC: a las 7 de la noche en Mexico
 * ya seria "mañana" y el filtro "Hoy" mostraria los de mañana.
 */
public interface HoyPort {

    LocalDate hoy();
}
