package com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface EscanerRemotoCasoUso {

    /** La PC abre una conexión para que un celular le mande un código de barras. {@code origen} = IP de la PC. */
    SesionAbierta abrir(String origen);

    /** El celular manda el código que escaneó. */
    void enviar(String sesionId, String codigo);

    /** La PC pregunta si llegó algo después de {@code desde}. */
    LecturaDeCodigos leer(String sesionId, long desde);

    /** La PC ya no necesita la conexión. Cerrar una que ya no existe no es error. */
    void cerrar(String sesionId);
}
