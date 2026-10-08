package com.ventas.key.hexagonal.stock.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.stock.dominio.puerto.salida.GuardarStockModeloPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>Un UPDATE de una sola columna: no carga la entidad {@code Producto} (con su codigo de barras y
 * su categoria) solo para cambiar el stock.
 */
@Repository
public class GuardarStockModeloJpaAdapter implements GuardarStockModeloPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void guardarStockTotal(Integer productoId, int stockTotal) {
        em.createNativeQuery("UPDATE producto SET stock = :stock WHERE id = :id")
                .setParameter("stock", stockTotal)
                .setParameter("id", productoId)
                .executeUpdate();
    }
}
