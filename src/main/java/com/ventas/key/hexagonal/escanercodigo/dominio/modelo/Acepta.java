package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

/**
 * Qué sabe recibir la pantalla de la PC que pregunta: códigos de barras, fotos y/o códigos de cliente de campaña (UUID). Así una foto no se
 * «entrega» a una pantalla sin dónde ponerla, ni un QR de cliente (UUID) a una pantalla que no tiene abierto el campo «Código del cliente».
 *
 * @param uuid la pantalla tiene abierta una receptora de código de cliente de campaña ({@code acepta=...,uuid}); solo así se le entregan UUID
 */
public record Acepta(boolean codigos, boolean fotos, boolean uuid) {

    public static final Acepta SOLO_CODIGOS = new Acepta(true, false, false);

    /** Sin códigos de cliente de campaña (como antes de 2026-11-10). */
    public Acepta(boolean codigos, boolean fotos) {
        this(codigos, fotos, false);
    }

    /** ¿Esta pantalla recibe este código? Un UUID solo si dice {@code uuid}; cualquier otro código, si dice {@code codigo}. */
    public boolean recibe(CodigoEscaneado c) {
        return SesionEscaner.esUuid(c.codigo()) ? uuid : codigos;
    }
}
