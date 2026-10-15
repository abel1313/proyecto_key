package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

import com.ventas.key.hexagonal.busquedapedido.dominio.excepcion.FiltroInvalidoException;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Lo que se escribio en el buscador (R1).
 * <ul>
 *   <li>Solo numeros: es un numero de pedido (exacto) o parte de un telefono. Un digito basta,
 *       porque hay pedidos #1 a #9.</li>
 *   <li>Con letras: minimo 3 caracteres. Con 1 o 2 el LIKE barre casi toda la base y el resultado no
 *       le sirve a nadie (regla de los buscadores en CLAUDE.md).</li>
 * </ul>
 */
public record TextoBuscado(String valor, boolean esNumero) {

    public static final int MINIMO_LETRAS = 3;
    public static final int MAXIMO = 100;

    public TextoBuscado {
        if (valor == null || valor.isBlank()) {
            throw new FiltroInvalidoException("El texto a buscar esta vacio");
        }
        if (valor.length() > MAXIMO) {
            throw new FiltroInvalidoException("El texto a buscar es muy largo (maximo " + MAXIMO + " caracteres)");
        }
        if (!esNumero && valor.length() < MINIMO_LETRAS) {
            throw new FiltroInvalidoException(
                    "Escribe al menos " + MINIMO_LETRAS + " letras para buscar por nombre, telefono, correo o articulo");
        }
    }

    /** {@code null} si no se escribio nada: buscar vacio es "sin filtro de texto". */
    public static TextoBuscado de(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        // "#120" se escribe asi en la card: se busca como numero.
        String sinGato = limpio.startsWith("#") ? limpio.substring(1).trim() : limpio;
        boolean esNumero = !sinGato.isEmpty() && sinGato.chars().allMatch(Character::isDigit);
        return new TextoBuscado(esNumero ? sinGato : limpio, esNumero);
    }

    /** El numero de pedido, si lo que se escribio cabe en uno. */
    public Integer comoNumeroDePedido() {
        if (!esNumero || valor.length() > 9) {
            return null;
        }
        return Integer.valueOf(valor);
    }

    /** Un numero corto no se busca dentro de los telefonos: "5" estaria en casi todos. */
    public boolean sirveParaTelefono() {
        return !esNumero || valor.length() >= MINIMO_LETRAS;
    }
}
