package com.ventas.key.hexagonal.flete.aplicacion.servicio;

import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.ConsultarFletesGuardadosCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.FletesGuardadosPort;
import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoConFlete;
import java.util.Collection;
import java.util.List;

/** [Hexagonal: Application] [Clean: Use Case] Orquesta la lectura; Java puro. */
public class ConsultarFletesGuardadosService implements ConsultarFletesGuardadosCasoUso {

    /** Un viaje o categoría grande no debe armar una consulta enorme: máximo de productos por llamada. */
    static final int MAXIMO = 2000;

    private final FletesGuardadosPort guardados;

    public ConsultarFletesGuardadosService(FletesGuardadosPort guardados) {
        this.guardados = guardados;
    }

    @Override
    public List<ProductoConFlete> deProductos(Collection<Integer> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return List.of();
        }
        return guardados.productosConFlete(productoIds.stream().distinct().limit(MAXIMO).toList());
    }
}
