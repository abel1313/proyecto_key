package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.util.List;

/**
 * Quién te compra. Solo ventas a clientes registrados (las de «cliente sin registro» no tienen historial) y sin contar las
 * ventas hechas al propio perfil de quien vende (mostrador).
 *
 * @param clientes    clientes distintos que compraron en todo el periodo
 * @param nuevos      de esos, los que compraron por primera vez dentro del periodo
 * @param porMes      un renglón por mes, todos seguidos y en orden, con ceros donde no hubo
 * @param mejores     los que más compraron en el periodo
 */
public record ClientesDelNegocio(int meses, int clientes, int nuevos, double total, List<MesDeClientes> porMes, List<ClienteVentas> mejores) {
    public ClientesDelNegocio {
        porMes = List.copyOf(porMes);
        mejores = List.copyOf(mejores);
    }

    public int recurrentes() {
        return clientes - nuevos;
    }
}
