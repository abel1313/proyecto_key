package com.ventas.key.hexagonal.flete.dominio.modelo;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Un viaje de compra: «hoy fui a comprar, el flete me costó $100 y traje estos productos».
 * Lleva su nombre y fecha, el flete total, gastos extra opcionales y los productos que trajo (piezas, costo y flete por pieza).
 *
 * <p>Reglas: el flete se da por cubierto cuando las piezas recuperan el «costo a cubrir» = flete total + los gastos que el dueño marcó como
 * «cuenta para cubrir». En esta fase solo se REGISTRA el viaje: lo que falta por asignar es el costo a cubrir menos el flete que ya llevan las
 * piezas; si las piezas llevan más flete que el costo, el exceso es ganancia (no se rechaza).
 */
public record ViajeDeCompra(Integer id, String nombre, LocalDate fecha, BigDecimal fleteTotal, String nota,
                            List<GastoDeViaje> gastos, List<ProductoDelViaje> productos) {

    public static final BigDecimal MONTO_MAXIMO = new BigDecimal("99999999.99");
    private static final int MAX_RENGLONES = 500;

    public ViajeDeCompra {
        nombre = nombre == null ? "" : nombre.trim();
        if (nombre.isEmpty()) {
            throw new ViajeInvalidoException("Falta el nombre del viaje (por ejemplo «Juguetes Reyes, viaje 1»).");
        }
        if (nombre.length() > 100) {
            throw new ViajeInvalidoException("El nombre del viaje es demasiado largo (máximo 100 letras).");
        }
        if (fecha == null) {
            throw new ViajeInvalidoException("Falta la fecha de la compra.");
        }
        fleteTotal = fleteTotal == null ? BigDecimal.ZERO.setScale(2) : fleteTotal.setScale(2, RoundingMode.HALF_UP);
        if (fleteTotal.signum() < 0) {
            throw new ViajeInvalidoException("El flete total no puede ser negativo.");
        }
        if (fleteTotal.compareTo(MONTO_MAXIMO) > 0) {
            throw new ViajeInvalidoException("El flete total es demasiado grande.");
        }
        nota = nota == null || nota.isBlank() ? null : nota.trim();
        if (nota != null && nota.length() > 255) {
            throw new ViajeInvalidoException("La nota es demasiado larga (máximo 255 letras).");
        }
        gastos = gastos == null ? List.of() : List.copyOf(gastos);
        productos = productos == null ? List.of() : List.copyOf(productos);
        if (productos.size() > MAX_RENGLONES) {
            throw new ViajeInvalidoException("Un viaje puede llevar máximo " + MAX_RENGLONES + " productos.");
        }
        Set<Integer> vistos = new HashSet<>();
        for (ProductoDelViaje p : productos) {
            if (!vistos.add(p.productoId())) {
                throw new ViajeInvalidoException("El producto " + (p.nombre() == null ? p.productoId() : "«" + p.nombre() + "»")
                        + " está dos veces en el viaje: junta sus piezas en un solo renglón.");
            }
        }
    }

    /** Lo que las piezas del viaje deben recuperar: el flete total más los gastos marcados «cuenta para cubrir». */
    public BigDecimal costoACubrir() {
        BigDecimal extra = gastos.stream().filter(GastoDeViaje::cuentaParaCubrir).map(GastoDeViaje::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return fleteTotal.add(extra).setScale(2, RoundingMode.HALF_UP);
    }

    /** Todos los gastos extra, cuenten o no para cubrir. */
    public BigDecimal gastosTotal() {
        return gastos.stream().map(GastoDeViaje::monto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public int piezasTotal() {
        return productos.stream().mapToInt(ProductoDelViaje::piezas).sum();
    }

    /** El flete que ya llevan las piezas de los productos (flete por pieza × piezas, sumado). */
    public BigDecimal fleteAsignado() {
        return productos.stream().map(ProductoDelViaje::fleteDelRenglon).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    /** Lo que costó comprar las piezas, sin flete. */
    public BigDecimal compraTotal() {
        return productos.stream().map(ProductoDelViaje::compraDelRenglon).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    /** Lo que falta de flete por repartir entre las piezas (nunca negativo: si ya sobra, es 0). */
    public BigDecimal faltaAsignar() {
        return costoACubrir().subtract(fleteAsignado()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** Flete que sobra de más repartido en las piezas (ganancia); 0 si no sobra. */
    public BigDecimal excedente() {
        return fleteAsignado().subtract(costoACubrir()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** El flete por pieza que sale de repartir el costo a cubrir entre todas las piezas del viaje (para sugerirlo; 0 si no hay piezas). */
    public BigDecimal fleteSugeridoPorPieza() {
        int piezas = piezasTotal();
        return piezas == 0 ? BigDecimal.ZERO.setScale(2) : costoACubrir().divide(BigDecimal.valueOf(piezas), 2, RoundingMode.HALF_UP);
    }

    public ViajeDeCompra conId(Integer nuevoId) {
        return new ViajeDeCompra(nuevoId, nombre, fecha, fleteTotal, nota, gastos, productos);
    }
}
