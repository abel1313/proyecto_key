package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;

import java.util.List;

/** Lo que trae un POST de Meta, ya traducido: mensajes nuevos y avisos de estado de los nuestros. */
record PayloadWhatsapp(List<MensajeEntrante> mensajes, List<EstadoDeEntrega> estados) {

    boolean vacio() {
        return mensajes.isEmpty() && estados.isEmpty();
    }
}
