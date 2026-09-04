package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ColorFlor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IColorFlorRepository extends BaseRepository<ColorFlor, Integer> {
    List<ColorFlor> findByTipoFlorIdAndActivoTrue(Integer tipoFlorId);

    // Usado para resincronizar ColorFlor.stock cuando se mueve el stock de su variante "sombra"
    // en un pedido (ver PedidoServiceImpl) -- sin esto, ColorFlor.stock (lo que ve el cliente en
    // el configurador) se queda desactualizado respecto al stock real de la variante.
    Optional<ColorFlor> findByVarianteId(Integer varianteId);
}
