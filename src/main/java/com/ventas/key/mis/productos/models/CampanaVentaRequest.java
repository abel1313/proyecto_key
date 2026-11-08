package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lo que Venta directa manda de la campaña (todo opcional; sin campaña vigente el back lo ignora):
 * {@code codigo} = UUID del cliente (escrito, pegado o leído del QR); {@code recurrenteManual} = el check «Cliente recurrente»;
 * {@code aceptaPrivacidad} = el cliente aceptó el aviso de privacidad (sin esto no se le crea código ni se guardan sus datos).
 */
@Getter
@Setter
@NoArgsConstructor
public class CampanaVentaRequest {
    private String codigo;
    private boolean recurrenteManual;
    private boolean aceptaPrivacidad;
}
