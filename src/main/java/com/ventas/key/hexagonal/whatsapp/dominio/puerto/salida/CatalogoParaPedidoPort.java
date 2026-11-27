package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ArticuloParaPedido;

import java.util.Optional;

/** Lee un artículo tal como está en la base (precio, stock, habilitado, imagen). [Hexagonal: Driven Port] */
public interface CatalogoParaPedidoPort {

    Optional<ArticuloParaPedido> articulo(int id);
}
