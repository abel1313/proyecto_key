package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.PlanInvalidoException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Las pruebas de QA que se preparan con un boton (R16 y R18, pedido del dueño 2026-10-13: "genera los
 * datos que necesita cada prueba para no perder tiempo creando pedidos"). Cada una crea pedidos
 * nuevos (catalogo y pedidos), asi una prueba no depende de lo que otra ya cancelo, pago o vendio.
 */
public enum EscenarioDePrueba {

    P12("12", "Lo legal: Art-200 y tickets para imprimir"),
    P14("14-QA", "🧩 Productos del modelo: stock 3 y un artículo con 1"),
    P15("15", "Habilitar un artículo viejo que guarda stock"),
    P16("16", "🕓 Pendiente, cambiarlo a Apartado y Entregas por zona"),
    P17("17", "Filtros de Tienda, precio $100 y Venta directa"),
    P18("18", "Agregar artículo: modelo deshabilitado y sin stock libre"),
    P19("19", "Errores con su código: rifa con el plazo vencido y código de compra ya usado"),
    P20("20", "Pedidos unidos: cambiar la forma de cobro del grupo, y un modelo agotado"),
    P21("21", "La fecha de entrega en Pedidos y en Entregas por zona"),
    P22("22", "🕓 Pendiente en Pago y Mis pedidos sin cancelados al abrir"),
    P24("24", "Unir un Ir pagando que ya tiene dinero"),
    P26("26", "Mis pedidos con caché: rápido y sin datos viejos");

    private final String prueba;
    private final String titulo;

    EscenarioDePrueba(String prueba, String titulo) {
        this.prueba = prueba;
        this.titulo = titulo;
    }

    public String prueba() {
        return prueba;
    }

    public String titulo() {
        return titulo;
    }

    /** Por el numero de la prueba ("21"). */
    public static EscenarioDePrueba de(String prueba) {
        String buscada = prueba == null ? "" : prueba.trim();
        return Arrays.stream(values())
                .filter(e -> e.prueba.equals(buscada))
                .findFirst()
                .orElseThrow(() -> new PlanInvalidoException("No hay datos para preparar la prueba " + buscada
                        + ". Se pueden preparar: "
                        + Arrays.stream(values()).map(e -> e.prueba).collect(Collectors.joining(", "))));
    }
}
