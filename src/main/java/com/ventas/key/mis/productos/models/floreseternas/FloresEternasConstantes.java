package com.ventas.key.mis.productos.models.floreseternas;

public final class FloresEternasConstantes {

    // Porcentaje de anticipo/enganche, usado en dos casos: (1) sobre el precio que el ADMIN le
    // asigna a una frase personalizada al validarla (ver RamoPedidoDetalleServiceImpl.validarFrase
    // -- no se conoce ni se cobra nada al cotizar, el anticipo real solo existe a partir de ese
    // momento), y (2) sobre el total del pedido cuando la entrega es urgente (ver
    // FlorPedidoServiceImpl.calcularPrecio -- este SI se conoce al cotizar).
    public static final double PORCENTAJE_ANTICIPO = 0.5;

    // Mensaje que ve el cliente al cotizar, mientras la frase personalizada sigue sin validar.
    // No menciona un monto porque todavia no existe -- eso se define despues, cuando el admin
    // le asigna precio, y se le avisa al cliente por separado.
    public static final String AVISO_FRASE_PENDIENTE =
            "Esta frase personalizada necesita ser aprobada por el equipo. Una vez asignado su "
          + "precio, se les contactará para pagar el anticipo del 50%. Una vez entregado el ramo "
          + "no hay reembolsos ni cancelaciones.";

    private FloresEternasConstantes() {
    }
}
