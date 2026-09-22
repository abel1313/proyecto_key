package com.ventas.key.hexagonal.stock.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;
import com.ventas.key.hexagonal.stock.dominio.puerto.salida.ConsultarStockPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>Una sola consulta por producto en vez de traer el producto y despues sus variantes: la
 * pantalla de alta de variantes la llama en cada carga, y esto se lee en una pasada.
 *
 * <p>Usa la misma regla que {@code VarianteServiceImpl.validarStockContraProducto()}: solo las
 * variantes con {@code habilitado = '1'} cuentan. Si las dos se separaran, la pantalla diria
 * "tenes 6 disponibles" y al guardar saltaria "Disponible: 0" -- que es exactamente el sintoma
 * que se corrigio el 2026-09-22.
 */
@Repository
@RequiredArgsConstructor
public class ConsultarStockJpaAdapter implements ConsultarStockPort {

    @PersistenceContext
    private EntityManager em;

    private static final String SELECT_DISPONIBILIDAD = """
            SELECT p.id,
                   p.nombre,
                   p.stock,
                   COALESCE(SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN v.habilitado = '1' THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN v.habilitado <> '1' THEN v.stock ELSE 0 END), 0)
            FROM producto p
            LEFT JOIN variantes v ON v.producto_id = p.id
            """;

    @Override
    public Optional<DisponibilidadStock> disponibilidadDe(Integer productoId) {
        List<Object[]> filas = em.createNativeQuery(
                        SELECT_DISPONIBILIDAD + " WHERE p.id = :productoId GROUP BY p.id, p.nombre, p.stock")
                .setParameter("productoId", productoId)
                .getResultList();

        return filas.isEmpty() ? Optional.empty() : Optional.of(aDominio(filas.get(0)));
    }

    @Override
    public List<DisponibilidadStock> productosDescuadrados() {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
                        SELECT_DISPONIBILIDAD
                        + " GROUP BY p.id, p.nombre, p.stock"
                        + " HAVING p.stock < COALESCE(SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END), 0)"
                        + " ORDER BY p.id")
                .getResultList();

        return filas.stream().map(ConsultarStockJpaAdapter::aDominio).toList();
    }

    private static DisponibilidadStock aDominio(Object[] fila) {
        return new DisponibilidadStock(
                entero(fila[0]),
                (String) fila[1],
                entero(fila[2]),
                entero(fila[3]),
                entero(fila[4]),
                entero(fila[5]));
    }

    /**
     * MySQL devuelve los SUM como BigDecimal o BigInteger segun el driver y la version, y el id
     * como Integer o Long. Se normaliza aca en vez de castear en cada posicion: un cast directo
     * revienta en tiempo de ejecucion la primera vez que el driver cambia de tipo.
     */
    private static int entero(Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }
}
