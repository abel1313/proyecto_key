package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Una fila del catálogo de zonas de entrega ({@code lugares_entrega}); una de ellas puede ser «recoger en el local». */
public record LugarDeEntrega(int id, String nombre, boolean recogerEnLocal) {
}
