package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.util.List;

/**
 * Respuesta a «¿llegó algo?»: los códigos nuevos, el número hasta el que ya se entregó (la PC lo manda en la siguiente pregunta)
 * si el celular sigue conectado (mandó señal de vida hace menos de 45 segundos) y la {@code epoca} de la conexión (cambia cuando la conexión se rehace
 * —venció y se abrió otra de la misma cuenta—: la PC vuelve a pedir desde el principio de la nueva y no se salta sus primeros códigos).
 */
public record LecturaDeCodigos(long ultimaSecuencia, List<CodigoEscaneado> codigos, boolean celularConectado, int epoca) {
}
