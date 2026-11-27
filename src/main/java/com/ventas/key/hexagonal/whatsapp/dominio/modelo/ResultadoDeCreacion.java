package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Cómo salió crear el Apartado con el servicio real de ventas. */
public sealed interface ResultadoDeCreacion {

    /** Se creó. {@code total} es el que cobró el sistema (puede ser menor que el del resumen si hubo precio de grupo o campaña); {@code lineas} son las REALES. */
    record Creado(int pedidoId, double total, java.util.List<LineaCobrada> lineas) implements ResultadoDeCreacion {
        public Creado {
            lineas = lineas == null ? java.util.List.of() : java.util.List.copyOf(lineas);
        }
    }

    /** Una línea tal como la cobró el servicio de ventas (con precio de grupo o campaña si hubo). */
    record LineaCobrada(String nombre, int cantidad, double precioNormal, double precioCobrado) {
        public double subtotal() {
            return precioCobrado * cantidad;
        }
    }

    /** Ya no se puede vender (se agotó o lo deshabilitaron mientras el cliente decidía). No es un error del sistema. */
    record NoDisponible(String motivo) implements ResultadoDeCreacion {
    }

    /** Falló algo del sistema: no se creó nada. {@code motivo} es técnico y solo lo lee el administrador. */
    record Fallo(String motivo) implements ResultadoDeCreacion {
    }
}
