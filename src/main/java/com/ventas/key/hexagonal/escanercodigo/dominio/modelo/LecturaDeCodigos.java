package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.util.List;

/** Respuesta a «¿llegó algo?»: los códigos nuevos y el número hasta el que ya se entregó (la PC lo manda en la siguiente pregunta). */
public record LecturaDeCodigos(long ultimaSecuencia, List<CodigoEscaneado> codigos) {
}
