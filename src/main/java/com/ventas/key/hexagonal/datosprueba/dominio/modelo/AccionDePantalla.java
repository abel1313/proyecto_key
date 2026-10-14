package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Una acción de una pantalla tal como la guarda Gestión de roles: la ruta de la pantalla
 * ("tienda/buscar") y la clave de la acción ("filtro-con-stock").
 */
public record AccionDePantalla(String ruta, String clave) {
}
