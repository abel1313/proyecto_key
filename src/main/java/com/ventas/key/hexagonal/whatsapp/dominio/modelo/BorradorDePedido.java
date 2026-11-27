package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * El pedido ya validado por el sistema y a la espera de que el cliente conteste «sí» (regla R16). Todo lo que trae
 * (nombres de artículos, precios, lugar) salió de la base; lo único que viene del cliente son sus datos de contacto.
 *
 * @param telefono       10 dígitos
 * @param lugar          null cuando recoge en el local y el catálogo no tiene esa fila
 * @param urlWhatsapp    liga directa a su WhatsApp ({@code https://wa.me/<número>})
 * @param resumenEnviadoEn hora en que el resumen SALIÓ al cliente (null = todavía no salió: un borrador así no se guarda)
 * @param irPagando      dijo que quiere ir pagando: el pedido igual es Apartado, con una nota para el dueño
 */
public record BorradorDePedido(List<LineaDePedido> lineas, String nombre, String telefono, String correo,
                               String recibe, String direccion, LugarDeEntrega lugar, boolean recogerEnLocal,
                               String observacionesCliente, boolean irPagando, String telefonoWhatsapp,
                               String urlWhatsapp, Instant creadoEn, Instant resumenEnviadoEn) {

    public BorradorDePedido {
        lineas = List.copyOf(lineas);
    }

    public BorradorDePedido conResumenEnviado(Instant cuando) {
        return new BorradorDePedido(lineas, nombre, telefono, correo, recibe, direccion, lugar, recogerEnLocal,
                observacionesCliente, irPagando, telefonoWhatsapp, urlWhatsapp, creadoEn, cuando);
    }

    public double total() {
        return lineas.stream().mapToDouble(LineaDePedido::subtotal).sum();
    }

    public boolean vigente(Instant ahora, Duration duracion) {
        return creadoEn != null && ahora.isBefore(creadoEn.plus(duracion));
    }

    /** Lo que se guarda en «observaciones» del pedido (regla R19). */
    public String observaciones() {
        StringBuilder sb = new StringBuilder("Pedido hecho por WhatsApp +").append(telefonoWhatsapp);
        if (observacionesCliente != null && !observacionesCliente.isBlank()) {
            sb.append(" | Nota del cliente: ").append(observacionesCliente.trim());
        }
        if (irPagando) {
            sb.append(" | El cliente quiere Ir pagando: cambiar a Ir pagando cuando llegue el enganche");
        }
        return sb.toString();
    }
}
