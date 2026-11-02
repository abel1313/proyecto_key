package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import java.time.Instant;

/** Cómo va una conexión: la PC lo pregunta antes de decidir si hace falta mostrar el QR o ya se puede escanear. */
public record EstadoDeSesion(boolean vigente, boolean celularConectado, long ultimaSecuencia, Instant venceEn) {
}
