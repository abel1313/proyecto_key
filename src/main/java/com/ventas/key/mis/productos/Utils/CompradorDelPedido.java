package com.ventas.key.mis.productos.Utils;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.Pedido;

/**
 * Quién es el comprador de un pedido: UNA sola regla para todas las pantallas (2026-10-13).
 *
 * <p>Una venta de mostrador guarda dos clientes en el pedido: {@code cliente} con la cuenta del admin
 * que atendió (el pedido necesita una) y {@code clienteSinRegistro} con la persona real a la que se le
 * vendió. El nombre que se escribió en la venta manda; el de quien hizo la venta solo se usa si la venta
 * no trae ninguno (pedido que hace el cliente desde su cuenta, o pedidos viejos). Así lo hace Mis pedidos
 * desde el 2026-09-22 ({@code COALESCE(csr.nombre_persona, nombre_receptor, c.nombre_persona)}); Entregas
 * por zona, Créditos / Abonos, Unir pedidos y el detalle lo hacían al revés y enseñaban al admin como
 * comprador.
 *
 * <p>Regla para el nombre: nombre de la venta, luego el receptor, luego la cuenta. Regla para el correo y
 * el teléfono: si la venta trae cliente sin registro, son SUS datos aunque estén vacíos -- nunca se cae a
 * los del admin, porque un aviso o un comprobante mandado ahí le llegaría a quien atendió y no al comprador.
 */
public final class CompradorDelPedido {

    private CompradorDelPedido() {
    }

    /** Nombre para mostrar: el de la venta, luego el receptor, luego la cuenta; null si no hay ninguno. */
    public static String nombre(Pedido pedido) {
        ClienteSinRegistro sinRegistro = pedido.getClienteSinRegistro();
        if (sinRegistro != null && hayTexto(sinRegistro.getNombrePersona())) {
            return sinRegistro.getNombrePersona();
        }
        if (hayTexto(pedido.getNombreReceptor())) {
            return pedido.getNombreReceptor().trim();
        }
        Cliente cuenta = pedido.getCliente();
        return cuenta != null ? cuenta.getNombrePersona() : null;
    }

    /**
     * Nombre de la persona dueña del correo y del teléfono: el del cliente sin registro si la venta lo
     * trae, y si no el de la cuenta. No mira el receptor: ese es a quién se le entrega y el detalle ya lo
     * enseña aparte ("Recibe:"). Sirve para saludar en un aviso y para el "Cliente" del detalle.
     */
    public static String nombreDelTitular(Pedido pedido) {
        ClienteSinRegistro sinRegistro = pedido.getClienteSinRegistro();
        if (sinRegistro != null) {
            return sinRegistro.getNombrePersona();
        }
        Cliente cuenta = pedido.getCliente();
        return cuenta != null ? cuenta.getNombrePersona() : null;
    }

    /** Correo del comprador (el de la venta si la trae; si no, el de la cuenta); null si no hay. */
    public static String correo(Pedido pedido) {
        ClienteSinRegistro sinRegistro = pedido.getClienteSinRegistro();
        if (sinRegistro != null) {
            return sinRegistro.getCorreoElectronico();
        }
        Cliente cuenta = pedido.getCliente();
        return cuenta != null ? cuenta.getCorreoElectronico() : null;
    }

    /** Teléfono del comprador (el de la venta si la trae; si no, el de la cuenta); null si no hay. */
    public static String telefono(Pedido pedido) {
        ClienteSinRegistro sinRegistro = pedido.getClienteSinRegistro();
        if (sinRegistro != null) {
            return sinRegistro.getNumeroTelefonico();
        }
        Cliente cuenta = pedido.getCliente();
        return cuenta != null ? cuenta.getNumeroTelefonico() : null;
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
