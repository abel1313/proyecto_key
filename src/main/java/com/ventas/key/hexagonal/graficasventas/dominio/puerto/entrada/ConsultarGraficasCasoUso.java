package com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClientesDelNegocio;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EntregasPorZona;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MapaDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.DineroPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EstadoDePedidos;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;

import java.time.LocalDate;
import java.util.List;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ConsultarGraficasCasoUso {

    /** Cómo te pagaron entre dos fechas (sin fechas: del día 1 del mes actual a hoy). Las ventas devueltas no cuentan. */
    VentasPorFormaDePago ventasPorFormaDePago(LocalDate desde, LocalDate hasta);

    /**
     * Ventas, ganancia, gastos y ganancia neta de los últimos {@code meses} meses, el actual incluido, del más viejo al más
     * nuevo. Siempre vienen todos los meses seguidos: uno sin movimiento sale en cero.
     */
    List<PuntoMensual> ventasYGastosPorMes(int meses);

    /**
     * Lo que te deben (Apartados e Ir pagando con saldo), por antigüedad desde la fecha del pedido: 0–7, 8–30 y más de 30 días,
     * y los {@code 8} que más tiempo llevan.
     */
    DineroPorCobrar dineroPorCobrar();

    /**
     * Mercancía habilitada con stock que no se ha vendido en {@code dias} días (y que ya tiene más de esos días en el catálogo),
     * a costo. De 7 a 365 días.
     */
    CapitalParado capitalParado(int dias);

    /**
     * Cuántos pedidos hay en cada punto del camino. Los abiertos se cuentan todos; los terminados (Entregado, Cancelado) solo los
     * de los últimos {@code dias} días (de 7 a 365).
     */
    EstadoDePedidos pedidosPorEstado(int dias);

    /** Cuándo se vende: por día de la semana y hora, en los últimos {@code dias} días contando hoy (de 7 a 365). Ventas devueltas no cuentan. */
    MapaDeVentas cuandoSeVende(int dias);

    /**
     * Quién te compra en los últimos {@code meses} meses (de 1 a 12, el actual incluido): clientes nuevos contra recurrentes por mes y los
     * 10 que más compraron.
     */
    ClientesDelNegocio clientes(int meses);

    /** A dónde van los pedidos no cancelados de los últimos {@code dias} días contando hoy (de 7 a 365). */
    EntregasPorZona entregasPorZona(int dias);
}
