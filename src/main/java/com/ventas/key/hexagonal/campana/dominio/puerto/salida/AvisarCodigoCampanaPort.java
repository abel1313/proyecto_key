package com.ventas.key.hexagonal.campana.dominio.puerto.salida;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;

/** [Hexagonal: Driven Port] Manda a la persona su código por correo. Si falla, la venta no se cae: solo se registra. */
public interface AvisarCodigoCampanaPort {

    void enviar(PersonaCampana persona, String nombreCampana);
}
