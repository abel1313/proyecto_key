package com.ventas.key.hexagonal.rifa.dominio.modelo;

/**
 * La red social donde el cliente participo.
 *
 * <p>Existe una copia propia en el dominio a proposito: el enum de la entidad JPA
 * ({@code BoletoRifa.Plataforma}) no se puede importar aca sin arrastrar
 * {@code jakarta.persistence} adentro del dominio. El adaptador traduce entre los dos.</p>
 *
 * <p>[Hexagonal: Domain Model] [Clean: Entity]</p>
 */
public enum Plataforma {
    FACEBOOK, INSTAGRAM, TIKTOK, OTRO
}
