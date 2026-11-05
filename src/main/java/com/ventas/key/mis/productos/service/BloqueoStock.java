package com.ventas.key.mis.productos.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Bloquea la fila de un articulo o producto Y la vuelve a leer antes de tocar su stock.
 *
 * <p>Por que no basta {@code findByIdWithLock}: si la entidad ya estaba en la sesion (por ejemplo el producto que trae el
 * articulo por EAGER, o los articulos que trae un pedido), Hibernate 6 manda el {@code SELECT ... FOR UPDATE} y espera
 * el bloqueo, pero devuelve la MISMA instancia con el valor que tenia antes de esperar. Y en MySQL REPEATABLE READ una
 * lectura sin bloqueo ve la "foto" del inicio de la transaccion. {@code refresh} con PESSIMISTIC_WRITE es una lectura
 * con bloqueo, que en InnoDB siempre ve lo ultimo confirmado, y sobreescribe el estado en memoria.
 *
 * <p>Se hace {@code flush} antes para no perder lo que esta misma transaccion ya cambio (dos lineas del mismo articulo).
 * Orden de bloqueo en todo el sistema: pedido -> articulo -> producto (evita bloqueos cruzados).
 */
@Component
public class BloqueoStock {

    @PersistenceContext
    private EntityManager em;

    public <T> T bloquear(T entidad) {
        if (entidad == null) {
            return null;
        }
        em.flush();
        em.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
        return entidad;
    }
}
