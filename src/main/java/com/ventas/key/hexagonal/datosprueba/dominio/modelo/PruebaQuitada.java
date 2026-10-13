package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Lo que se quitó de una prueba para dejarla en blanco (R21): los pedidos se cancelan con el servicio
 * real (el stock regresa) y los modelos y artículos se dan de baja, nunca se borran.
 *
 * @param pedidosCancelados  cuántos pedidos de la prueba se cancelaron
 * @param pedidosSinCancelar cuántos no se pudieron cancelar (se dejan como están; el motivo va al log)
 * @param modelosDadosDeBaja cuántos modelos de la prueba se dieron de baja
 * @param error              null si salió bien; en "Quitar todas", el motivo si esa prueba falló
 */
public record PruebaQuitada(String prueba, String titulo, int pedidosCancelados, int pedidosSinCancelar,
                            int modelosDadosDeBaja, String error) {

    public static PruebaQuitada conError(EscenarioDePrueba escenario, String motivo) {
        return new PruebaQuitada(escenario.prueba(), escenario.titulo(), 0, 0, 0, motivo);
    }

    /** True si no había nada de la vez anterior. */
    public boolean estabaVacia() {
        return pedidosCancelados == 0 && pedidosSinCancelar == 0 && modelosDadosDeBaja == 0;
    }

    /** Una línea para el dueño, o null si no había nada que quitar. */
    public String resumen() {
        if (estabaVacia()) {
            return null;
        }
        StringBuilder texto = new StringBuilder("Se quitó lo de antes: ");
        texto.append(pedidosCancelados).append(pedidosCancelados == 1 ? " pedido cancelado" : " pedidos cancelados");
        texto.append(" y ").append(modelosDadosDeBaja).append(modelosDadosDeBaja == 1 ? " modelo dado de baja" : " modelos dados de baja");
        if (pedidosSinCancelar > 0) {
            texto.append(" (").append(pedidosSinCancelar).append(pedidosSinCancelar == 1 ? " pedido" : " pedidos")
                    .append(" no se pudo cancelar: ver el log)");
        }
        return texto.append('.').toString();
    }
}
