package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.time.Instant;

/** Lo que la PC recibe al abrir la conexión: el identificador que va en el QR y hasta cuándo vale. */
public record SesionAbierta(String id, Instant venceEn) {
}
