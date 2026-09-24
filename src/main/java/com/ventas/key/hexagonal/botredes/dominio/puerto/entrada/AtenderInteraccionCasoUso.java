package com.ventas.key.hexagonal.botredes.dominio.puerto.entrada;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;
import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;

/**
 * Lo que el bot de redes sabe hacer, de cara al webhook de Meta.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Input Boundary]</p>
 */
public interface AtenderInteraccionCasoUso {

    /** Un comentario o mensaje directo nuevo. Si lo escribió la propia cuenta del negocio, se trata como respuesta del admin. */
    void atender(Interaccion interaccion);

    /** Un mensaje directo que mandó la cuenta del negocio (eco): si no lo mandó el bot, lo contestó el admin a mano. */
    void registrarEcoDeMensaje(RedSocial red, String mid, String clienteId);
}
