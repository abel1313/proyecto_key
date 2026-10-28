package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reglas de meses sin intereses (M1 a M6), sin saber nada de Spring ni de la base.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <ul>
 *   <li>M1 la casilla "acepta MSI" esta apagada por default, por producto y por articulo;</li>
 *   <li>M2 solo se ofrece MSI si TODOS los articulos del pedido lo aceptan;</li>
 *   <li>M3 minimo de compra (default $300, configurable);</li>
 *   <li>M4 solo los planes activos de 3, 6, 9 y 12 meses;</li>
 *   <li>M5 Ir pagando y Apartado nunca tienen MSI;</li>
 *   <li>M6 el back manda a la pasarela el tope de cuotas permitido (nunca confia en el front).</li>
 * </ul>
 */
public final class ReglasMsi {

    public static final BigDecimal MINIMO_POR_DEFECTO = new BigDecimal("300");
    private static final List<Integer> PLAZOS_VALIDOS = List.of(3, 6, 9, 12);

    private ReglasMsi() {
    }

    /** Los meses que se pueden ofrecer para este pedido; vacio si no aplica MSI. */
    public static List<Integer> mesesOfrecidos(TipoDeCobro tipo, BigDecimal total, List<ArticuloACobrar> articulos,
                                               List<PlanMsi> planes, BigDecimal minimo) {
        if (tipo == null || !tipo.admiteMsi() || total == null || articulos == null || articulos.isEmpty()) {
            return List.of();
        }
        BigDecimal piso = minimo != null ? minimo : MINIMO_POR_DEFECTO;
        if (total.compareTo(piso) < 0) {
            return List.of();
        }
        if (!articulos.stream().allMatch(ArticuloACobrar::aceptaMsi)) {
            return List.of();
        }
        return planes.stream()
                .filter(PlanMsi::activo)
                .map(PlanMsi::meses)
                .filter(PLAZOS_VALIDOS::contains)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Igual que {@link #mesesOfrecidos}, pero cuando no hay meses dice <b>por qué</b> (tipo de pedido, mínimo de
     * compra, qué artículos no los aceptan o que no hay planes activos). Los plazos son exactamente los mismos.
     */
    public static OfertaDeMeses ofertaDeMeses(TipoDeCobro tipo, BigDecimal total, List<ArticuloACobrar> articulos,
                                              List<PlanMsi> planes, BigDecimal minimo) {
        if (tipo == null || !tipo.admiteMsi()) {
            return OfertaDeMeses.sinMeses("Este tipo de pedido se paga en efectivo o transferencia: "
                    + "no admite meses sin intereses");
        }
        if (articulos == null || articulos.isEmpty()) {
            return OfertaDeMeses.sinMeses("El pedido no tiene artículos");
        }
        BigDecimal piso = minimo != null ? minimo : MINIMO_POR_DEFECTO;
        if (total == null || total.compareTo(piso) < 0) {
            return OfertaDeMeses.sinMeses("La compra debe ser de al menos " + pesos(piso)
                    + " para ofrecer meses sin intereses");
        }
        List<String> sinMsi = articulos.stream().filter(a -> !a.aceptaMsi()).map(ArticuloACobrar::nombre).toList();
        if (!sinMsi.isEmpty()) {
            return OfertaDeMeses.sinMeses("Este artículo no acepta meses sin intereses, por eso el pedido se paga "
                    + "de contado: " + String.join(", ", sinMsi), sinMsi);
        }
        List<Integer> meses = mesesOfrecidos(tipo, total, articulos, planes, minimo);
        if (meses.isEmpty()) {
            return OfertaDeMeses.sinMeses("No hay planes de meses sin intereses activos");
        }
        return OfertaDeMeses.con(meses);
    }

    /** Valida las cuotas pedidas (1 = contado) y explica con claridad por que no se puede. */
    public static void validarCuotas(int cuotas, TipoDeCobro tipo, BigDecimal total, List<ArticuloACobrar> articulos,
                                     List<PlanMsi> planes, BigDecimal minimo) {
        if (cuotas < 1) {
            throw PagoConTarjetaException.cuotasInvalidas(cuotas);
        }
        if (cuotas == 1) {
            return;
        }
        if (tipo == null || !tipo.admiteMsi()) {
            throw PagoConTarjetaException.msiNoPermitido("este tipo de pedido no los admite");
        }
        BigDecimal piso = minimo != null ? minimo : MINIMO_POR_DEFECTO;
        if (total == null || total.compareTo(piso) < 0) {
            throw PagoConTarjetaException.msiNoPermitido("la compra debe ser de al menos " + pesos(piso));
        }
        List<String> sinMsi = articulos == null ? List.of() : articulos.stream()
                .filter(a -> !a.aceptaMsi()).map(ArticuloACobrar::nombre).toList();
        if (articulos == null || articulos.isEmpty() || !sinMsi.isEmpty()) {
            String cuales = sinMsi.isEmpty() ? "" : ": " + sinMsi.stream().collect(Collectors.joining(", "));
            throw PagoConTarjetaException.msiNoPermitido("este artículo no acepta meses sin intereses" + cuales);
        }
        if (!mesesOfrecidos(tipo, total, articulos, planes, minimo).contains(cuotas)) {
            throw PagoConTarjetaException.msiNoPermitido(
                    "el plan de " + cuotas + " meses no está disponible");
        }
    }

    private static String pesos(BigDecimal monto) {
        return PagoConTarjetaException.pesos(monto);
    }
}
