package com.ventas.key.hexagonal.campana.dominio.modelo;

/** [Hexagonal: Domain] Cómo se cobra la venta. El precio de campaña solo entra en Contado y Apartado, nunca en Ir pagando. */
public enum FormaDeCobro {
    CONTADO, APARTADO, IR_PAGANDO;

    public boolean admiteDescuentoDeCampana() {
        return this != IR_PAGANDO;
    }
}
