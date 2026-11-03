package com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EsperaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EstadoDeSesion;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface EscanerRemotoCasoUso {

    /** La PC abre una conexión para que un celular le mande códigos de barras. {@code origen} = IP (o usuario) de la PC. */
    SesionAbierta abrir(String origen);

    /** Cómo va la conexión (para reutilizar la que ya estaba enlazada en lugar de pedir otro QR). */
    EstadoDeSesion estado(String sesionId);

    /** El celular manda el código que escaneó. Devuelve si la PC lo estaba esperando (si no, el celular avisa «pulsa 📱 en la computadora»). */
    boolean enviar(String sesionId, String codigo);

    /** El celular avisa que sigue ahí (cada ~15 s): así la PC sabe que ya puede escanear sin QR. */
    void latido(String sesionId);

    /** La PC pregunta, sin esperar, si llegó algo después de {@code desde}. */
    LecturaDeCodigos leer(String sesionId, long desde);

    /** La PC deja la pregunta abierta: se contesta en cuanto llegue un código después de {@code desde}. */
    EsperaDeCodigos esperar(String sesionId, long desde);

    /** La PC ya no necesita la conexión. Cerrar una que ya no existe no es error. */
    void cerrar(String sesionId);
}
