package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import java.time.LocalDateTime;
import java.util.List;

/** Lo que se guarda de un grupo: quien esta y quien es el titular. Los montos viven en los pedidos. */
public record RegistroGrupo(
        Integer grupoId,
        Integer pedidoTitularId,
        boolean activo,
        LocalDateTime creado,
        String nota,
        List<Integer> pedidoIds) {
}
