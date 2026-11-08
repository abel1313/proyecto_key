package com.ventas.key.hexagonal.campana.dominio.modelo;

/** [Hexagonal: Domain] Lo que quedó al registrar la compra en la campaña: a quién se ligó, si se acaba de crear y de qué campaña. */
public record RegistroDeCompra(PersonaCampana persona, boolean nueva, String nombreCampana) { }
