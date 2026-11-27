package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.List;

/**
 * Lo que el modelo de IA escribió en la marca {@code ##PEDIDO{...}##} cuando ya tiene los datos del cliente.
 * Todo es texto sin validar: puede venir incompleto, mal escrito o inventado (regla R16). El precio NO viaja aquí
 * a propósito: nunca lo decide el modelo.
 *
 * @param usarTelefonoDeWhatsapp el cliente confirmó que lo contacten al número desde el que escribe
 * @param recogerEnLocal         el cliente recoge en el local (no hay lugar de entrega ni dirección)
 * @param irPagando              el cliente dijo que quiere ir pagando o dar un enganche (el pedido igual nace Apartado)
 * @param usarDatosPrevios       el cliente confirmó usar el nombre y el correo (y la dirección si no escribió otra) que ya dio
 *                               antes desde ESTE número: el modelo solo los ve enmascarados, el sistema pone los reales
 */
public record SolicitudDePedido(List<LineaSolicitada> articulos, String nombre, boolean usarTelefonoDeWhatsapp,
                                String telefono, String correo, String recibe, String direccion, String lugar,
                                boolean recogerEnLocal, String observaciones, boolean irPagando,
                                boolean usarDatosPrevios) {

    public SolicitudDePedido {
        articulos = articulos == null ? List.of() : List.copyOf(articulos);
    }
}
