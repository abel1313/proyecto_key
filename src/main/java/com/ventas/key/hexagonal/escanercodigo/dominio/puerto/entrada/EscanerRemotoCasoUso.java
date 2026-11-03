package com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.Acepta;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EsperaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EstadoDeSesion;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface EscanerRemotoCasoUso {

    /** La PC abre una conexión para que un celular le mande códigos de barras. {@code origen} = IP (o usuario) de la PC. */
    SesionAbierta abrir(String origen);

    /**
     * El canal de una CUENTA: la PC y el celular de la misma persona (los dos con sesión iniciada) se encuentran solos, sin QR. Devuelve
     * la conexión vigente de ese usuario o abre una nueva; si vence, la siguiente llamada abre otra y los dos lados convergen en ella.
     */
    String idDelCanal(String usuario);

    /** Cómo va la conexión (para reutilizar la que ya estaba enlazada en lugar de pedir otro QR). */
    EstadoDeSesion estado(String sesionId);

    /** El celular manda el código que escaneó. Devuelve si la PC lo estaba esperando (si no, el celular avisa «pulsa 📱 en la computadora»). */
    boolean enviar(String sesionId, String codigo);

    /** El celular avisa que sigue ahí (cada ~15 s): así la PC sabe que ya puede escanear sin QR. */
    void latido(String sesionId);

    /** La PC pregunta, sin esperar, si llegó algo después de {@code desde}. */
    LecturaDeCodigos leer(String sesionId, long desde);

    /** Igual, respetando lo que la pantalla sabe recibir. */
    LecturaDeCodigos leer(String sesionId, long desde, Acepta acepta);

    /** La PC deja la pregunta abierta: se contesta en cuanto llegue un código después de {@code desde}. */
    EsperaDeCodigos esperar(String sesionId, long desde);

    /** Igual, diciendo qué sabe recibir la pantalla (códigos, fotos o las dos). */
    EsperaDeCodigos esperar(String sesionId, long desde, Acepta acepta);

    /**
     * El celular manda una FOTO (JPEG en base64 con encabezado data:). Solo el canal de la cuenta las acepta; una conexión por QR (pública) lanza
     * {@code FotoNoPermitida}. Devuelve si la PC la estaba esperando.
     */
    boolean enviarFoto(String sesionId, String foto);

    /** La PC ya no necesita la conexión. Cerrar una que ya no existe no es error. */
    void cerrar(String sesionId);
}
