package com.ventas.key.hexagonal.campana.dominio.puerto.salida;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;

/** [Hexagonal: Driven Port] Manda a la persona su código por correo. Si falla, la venta no se cae: solo se registra. */
public interface AvisarCodigoCampanaPort {

    /** Primer envío a la persona (a su correo). */
    default void enviar(PersonaCampana persona, String nombreCampana) {
        enviar(persona, nombreCampana, persona.correo(), true);
    }

    /**
     * @param correoDestino a dónde sale (el de la venta, o el de la persona); si es null o vacío no se manda nada
     * @param primeraVez    true si la persona se acaba de crear; false cuando se le vuelve a mandar su código
     */
    void enviar(PersonaCampana persona, String nombreCampana, String correoDestino, boolean primeraVez);
}
