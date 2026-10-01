package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

/** [Hexagonal: Driven Port] Para R1: en que base de datos se esta escribiendo. */
public interface AmbientePort {

    String BASE_DE_PRUEBAS = "inventario_key_qa";

    String nombreDeLaBase();
}
