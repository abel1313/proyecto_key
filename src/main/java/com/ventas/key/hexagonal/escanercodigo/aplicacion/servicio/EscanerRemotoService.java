package com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionEscaner;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada.EscanerRemotoCasoUso;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida.SesionesEscanerPort;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * [Hexagonal: Use Case] [Clean: Use Cases] Java puro, sin Spring.
 *
 * <p>Límites contra abuso (los endpoints son públicos porque la tienda también tiene buscador por código):
 * E6 máximo 2000 conexiones vigentes en total y 10 por PC (IP); el identificador es aleatorio de 128 bits y solo va en el QR.
 */
public class EscanerRemotoService implements EscanerRemotoCasoUso {

    public static final int MAXIMO_TOTAL = 2000;
    public static final int MAXIMO_POR_ORIGEN = 10;

    private final SesionesEscanerPort sesiones;
    private final Clock reloj;
    private final Supplier<String> generadorDeIds;

    public EscanerRemotoService(SesionesEscanerPort sesiones, Clock reloj, Supplier<String> generadorDeIds) {
        this.sesiones = sesiones;
        this.reloj = reloj;
        this.generadorDeIds = generadorDeIds;
    }

    @Override
    public synchronized SesionAbierta abrir(String origen) {   // synchronized: contar y guardar sin que otra solicitud se cuele en medio
        Instant ahora = reloj.instant();
        String de = origen == null || origen.isBlank() ? "desconocido" : origen;
        if (sesiones.vigentes(ahora) >= MAXIMO_TOTAL) {
            throw new EscanerException.DemasiadasSolicitudes("Hay demasiadas conexiones con celulares abiertas. Intenta en unos minutos.");
        }
        if (sesiones.vigentesDe(de, ahora) >= MAXIMO_POR_ORIGEN) {
            throw new EscanerException.DemasiadasSolicitudes("Ya tienes varias conexiones con celulares abiertas. Cierra alguna o espera unos minutos.");
        }
        SesionEscaner nueva = new SesionEscaner(generadorDeIds.get(), de, ahora);
        sesiones.guardar(nueva);
        return new SesionAbierta(nueva.id(), nueva.venceEn());
    }

    @Override
    public void enviar(String sesionId, String codigo) {
        encontrar(sesionId).registrar(codigo, reloj.instant());
    }

    @Override
    public LecturaDeCodigos leer(String sesionId, long desde) {
        return encontrar(sesionId).leer(Math.max(0, desde), reloj.instant());
    }

    @Override
    public void cerrar(String sesionId) {
        sesiones.buscar(sesionId).ifPresent(s -> {
            s.cerrar();
            sesiones.eliminar(sesionId);
        });
    }

    private SesionEscaner encontrar(String sesionId) {
        return sesiones.buscar(sesionId == null ? "" : sesionId).orElseThrow(EscanerException.SesionNoDisponible::new);
    }
}
