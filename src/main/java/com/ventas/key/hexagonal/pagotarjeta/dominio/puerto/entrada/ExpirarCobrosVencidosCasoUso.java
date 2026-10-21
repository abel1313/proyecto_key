package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ExpirarCobrosVencidosCasoUso {

    /**
     * T7: revisa los cobros activos que ya pasaron su vigencia y los resuelve (consulta a la pasarela
     * antes de decidir). Lo llama un trabajo programado.
     *
     * @return cuantos dejaron de estar activos
     */
    int expirarVencidos();
}
