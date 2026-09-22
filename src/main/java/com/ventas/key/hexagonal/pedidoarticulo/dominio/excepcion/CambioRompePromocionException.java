package com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PromocionDelPedido;

import java.util.List;

/**
 * El articulo nuevo no pertenece a la promocion de la linea que se queria cambiar (R4).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p><b>Esto no es un error: es una pregunta.</b> El dominio no elige por el negocio entre romper
 * el combo y conservarlo, porque las dos salidas son validas y solo quien esta atendiendo sabe
 * cual corresponde. Por eso la excepcion carga los datos que necesita el modal para preguntarlo:
 * que promocion es, que lineas se irian si se quita, y cuanto se deja de descontar.
 *
 * <p>Si esto se resolviera solo, cualquiera de las dos decisiones estaria mal la mitad de las
 * veces -- y seria una decision de dinero tomada en silencio.
 */
public class CambioRompePromocionException extends EdicionPedidoException {

    private final transient PromocionDelPedido promocion;
    private final transient List<ArticuloDePedido> lineasDelCombo;
    private final transient String nombreArticuloNuevo;

    public CambioRompePromocionException(PromocionDelPedido promocion,
                                         List<ArticuloDePedido> lineasDelCombo,
                                         String nombreArticuloNuevo) {
        super(String.format(
                "'%s' no forma parte de la promocion '%s'. Para llevarlo hay que quitar la promocion "
                        + "completa (%d articulo(s)) o conservarla y agregarlo aparte a precio normal",
                nombreArticuloNuevo, promocion.descripcion(), lineasDelCombo.size()));
        this.promocion = promocion;
        this.lineasDelCombo = List.copyOf(lineasDelCombo);
        this.nombreArticuloNuevo = nombreArticuloNuevo;
    }

    public PromocionDelPedido promocion() {
        return promocion;
    }

    /** Las lineas que se irian con la opcion (a) -- el combo entero, no solo la que se cambiaba. */
    public List<ArticuloDePedido> lineasDelCombo() {
        return lineasDelCombo;
    }

    public String nombreArticuloNuevo() {
        return nombreArticuloNuevo;
    }

    /** Lo que se dejaria de cobrar al sacar el combo, para poder mostrarlo en el modal. */
    public double importeDelCombo() {
        return lineasDelCombo.stream().mapToDouble(ArticuloDePedido::subTotal).sum();
    }
}
