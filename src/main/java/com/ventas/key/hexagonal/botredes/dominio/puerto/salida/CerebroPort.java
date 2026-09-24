package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;

/**
 * El chatbot. Devuelve el texto crudo, con sus marcas (##ESCALAR##, ##FAREWELL##, ##BUSCAR[..]##).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface CerebroPort {

    /** Comentario en una publicación ligada a un producto: contesta solo sobre ese producto. */
    String responderSobreProducto(RedSocial red, String texto, Integer varianteId, boolean esPrimeraVez);

    /** Comentario en una publicación sin producto: agradece saludos y avisos; cualquier pregunta se escala. */
    String responderComentarioSinProducto(RedSocial red, String texto, boolean esPrimeraVez);

    /** Mensaje directo: contesta con todo el catálogo. */
    String responderMensajeDirecto(RedSocial red, String texto, boolean esPrimeraVez);
}
