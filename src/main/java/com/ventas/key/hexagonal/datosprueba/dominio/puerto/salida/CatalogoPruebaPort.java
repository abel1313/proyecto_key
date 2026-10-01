package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;

import java.util.List;

/** [Hexagonal: Driven Port] Modelos y articulos de prueba en la base. */
public interface CatalogoPruebaPort {

    /** R5: el siguiente numero libre despues del codigo 2098... mas alto que ya exista. */
    long siguienteNumero();

    /** Da de alta las categorias que falten. */
    void asegurarCategorias(List<String> categorias);

    /** R7: cuantas imagenes reales hay para reusar. */
    int imagenesDisponibles();

    /**
     * R12: guarda un lote en una sola transaccion (modelos, articulos, codigos de barras e imagenes)
     * y regresa los articulos guardados.
     */
    List<ArticuloGuardado> guardarLote(List<ModeloDePrueba> modelos);

    /** R14. Regresa cuantos articulos quedaron de baja. */
    int darDeBaja();
}
