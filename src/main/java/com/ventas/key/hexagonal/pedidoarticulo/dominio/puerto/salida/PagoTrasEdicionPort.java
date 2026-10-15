package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

/**
 * Deja el pedido como corresponde a sus abonos despues de editar sus articulos (R9).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>La cuenta es solo de ese pedido: si queda cubierto pasa a PAGADO, si deja de estarlo regresa
 * a Apartado / Ir pagando, y lo que sobre es saldo a favor de su cliente. Nunca se pasa dinero a
 * otros pedidos del grupo.
 */
public interface PagoTrasEdicionPort {

    void ajustarTrasEditar(Integer pedidoId);
}
