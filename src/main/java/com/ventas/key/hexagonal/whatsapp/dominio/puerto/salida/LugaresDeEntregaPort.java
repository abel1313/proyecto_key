package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LugarDeEntrega;

import java.util.List;

/** El catálogo de zonas de entrega (Sistema → lugares de entrega). [Hexagonal: Driven Port] */
public interface LugaresDeEntregaPort {

    List<LugarDeEntrega> todos();
}
