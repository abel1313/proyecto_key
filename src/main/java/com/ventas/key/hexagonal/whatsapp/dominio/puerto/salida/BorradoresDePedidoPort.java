package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;

import java.util.Optional;

/**
 * Guarda el pedido ya validado mientras el cliente contesta «sí» (regla R16). Un borrador por conversación: guardar
 * uno nuevo reemplaza al anterior. [Hexagonal: Driven Port]
 */
public interface BorradoresDePedidoPort {

    void guardar(long clienteId, BorradorDePedido borrador);

    /** El borrador de esa conversación, si existe (aunque ya haya caducado: quien llama decide con {@code vigente}). */
    Optional<BorradorDePedido> porCliente(long clienteId);

    void descartar(long clienteId);
}
