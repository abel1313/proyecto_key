package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoDeCreacion;

/**
 * Crea el Apartado con el servicio REAL de ventas (el mismo de «Venta directa»), nunca con un INSERT. Si algo falla no
 * se crea nada. No lanza excepción: devuelve el resultado. [Hexagonal: Driven Port]
 */
public interface PedidosApartadoPort {

    ResultadoDeCreacion crearApartado(BorradorDePedido borrador);

    /**
     * Cuántos Apartados siguen abiertos (no cancelados, no entregados) hechos por el bot desde ese número de WhatsApp
     * (se reconocen por las observaciones «Pedido hecho por WhatsApp +número»).
     */
    int contarAbiertosDesde(String numeroDeWhatsapp);
}
