package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Como va (o como termino) la ultima corrida. Inmutable: cada cambio es un Avance nuevo, asi quien
 * lo consulta desde otro hilo nunca ve uno a medio escribir.
 */
public record Avance(
        Estado estado,
        String fase,
        int modelosPedidos,
        int modelosCreados,
        int articulosCreados,
        int pedidosPedidos,
        int pedidosCreados,
        int pedidosConError,
        String ultimoError,
        String aviso,
        LocalDateTime inicio,
        LocalDateTime fin) {

    public enum Estado { SIN_CORRER, EN_CURSO, TERMINADO, FALLO }

    public static Avance sinCorrer() {
        return new Avance(Estado.SIN_CORRER, "Todavía no se ha generado nada", 0, 0, 0, 0, 0, 0, null, null, null, null);
    }

    public static Avance arranque(PlanDeDatos plan) {
        return new Avance(Estado.EN_CURSO, "Preparando", plan.modelos(), 0, 0, plan.pedidos(), 0, 0, null, null,
                LocalDateTime.now(), null);
    }

    public boolean enCurso() {
        return estado == Estado.EN_CURSO;
    }

    public Avance conFase(String nuevaFase) {
        return new Avance(estado, nuevaFase, modelosPedidos, modelosCreados, articulosCreados, pedidosPedidos,
                pedidosCreados, pedidosConError, ultimoError, aviso, inicio, fin);
    }

    public Avance conCatalogo(int modelos, int articulos) {
        return new Avance(estado, "Creando modelos y artículos", modelosPedidos, modelos, articulos, pedidosPedidos,
                pedidosCreados, pedidosConError, ultimoError, aviso, inicio, fin);
    }

    public Avance conPedidos(int creados, int conError, String error) {
        return new Avance(estado, "Creando pedidos", modelosPedidos, modelosCreados, articulosCreados, pedidosPedidos,
                creados, conError, error != null ? error : ultimoError, aviso, inicio, fin);
    }

    public Avance conAviso(String nuevoAviso) {
        return new Avance(estado, fase, modelosPedidos, modelosCreados, articulosCreados, pedidosPedidos,
                pedidosCreados, pedidosConError, ultimoError, nuevoAviso, inicio, fin);
    }

    public Avance terminado() {
        return new Avance(Estado.TERMINADO, "Terminado", modelosPedidos, modelosCreados, articulosCreados,
                pedidosPedidos, pedidosCreados, pedidosConError, ultimoError, aviso, inicio, LocalDateTime.now());
    }

    public Avance fallo(String motivo) {
        return new Avance(Estado.FALLO, "Se detuvo: " + motivo, modelosPedidos, modelosCreados, articulosCreados,
                pedidosPedidos, pedidosCreados, pedidosConError, motivo, aviso, inicio, LocalDateTime.now());
    }
}
