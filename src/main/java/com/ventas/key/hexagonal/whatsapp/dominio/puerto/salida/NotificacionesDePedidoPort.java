package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoDeCreacion;

/**
 * Los correos del pedido por WhatsApp (regla R20). Salen en otro hilo (un SMTP lento no retiene el candado del número) y
 * ninguno lanza excepción: un correo que no sale nunca tira el pedido ni la respuesta al cliente. [Hexagonal: Driven Port]
 */
public interface NotificacionesDePedidoPort {

    /** El ticket al correo del cliente, con las líneas y el total REALES. @return true si quedó en camino (se encoló) */
    boolean ticketAlCliente(BorradorDePedido borrador, ResultadoDeCreacion.Creado creado);

    /** La venta al correo del dueño, con las líneas y el total REALES. */
    void ventaAlAdministrador(BorradorDePedido borrador, ResultadoDeCreacion.Creado creado);

    /** No se pudo crear el pedido: lo ocurrido y los datos que ya se tenían (sin tokens). */
    void falloAlAdministrador(BorradorDePedido borrador, String motivo);

    /** El cliente pidió cancelar un pedido: el bot no cancela, solo anota el número y avisa. */
    void cancelacionAlAdministrador(String telefonoWhatsapp, String nombre, int numeroDePedido);

    /** Un número llegó al tope de Apartados abiertos: no se creó otro. */
    void topeAlAdministrador(String telefonoWhatsapp, String nombre, int abiertos);
}
