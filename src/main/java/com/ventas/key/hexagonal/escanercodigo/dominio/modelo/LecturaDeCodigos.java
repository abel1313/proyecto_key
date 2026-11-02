package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.util.List;

/**
 * Respuesta a «¿llegó algo?»: los códigos nuevos, el número hasta el que ya se entregó (la PC lo manda en la siguiente pregunta)
 * y si el celular sigue conectado (mandó señal de vida hace menos de 45 segundos).
 */
public record LecturaDeCodigos(long ultimaSecuencia, List<CodigoEscaneado> codigos, boolean celularConectado) {
}
