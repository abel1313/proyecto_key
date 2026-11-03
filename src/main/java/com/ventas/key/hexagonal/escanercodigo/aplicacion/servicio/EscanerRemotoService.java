package com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.Acepta;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EsperaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EstadoDeSesion;
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
    /**
     * Los orígenes llevan un prefijo que dice de dónde vienen, para que ninguno pueda hacerse pasar por otro (p. ej. un visitante que falsifique
     * X-Forwarded-For con «canal:ana»): {@code ip:} visitante sin sesión, {@code usuario:} conexión por QR abierta por alguien con sesión (cuenta para
     * su propio límite) y {@code canal:} el canal de la cuenta (el celular y la PC de la misma persona, sin QR).
     */
    public static final String PREFIJO_IP = "ip:";
    public static final String PREFIJO_USUARIO = "usuario:";
    public static final String PREFIJO_CANAL = "canal:";

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
        if (sesiones.vigentesDe(de, ahora) >= MAXIMO_POR_ORIGEN) {
            throw new EscanerException.DemasiadasSolicitudes("Ya tienes varias conexiones con celulares abiertas. Cierra alguna o espera unos minutos.");
        }
        // El cupo total es de las conexiones ANÓNIMAS: las del personal con sesión (origen «usuario:…») cuentan solo en su propio límite,
        // así que nadie desde fuera puede dejarlas sin servicio llenando el cupo global.
        if (!de.startsWith(PREFIJO_USUARIO) && !de.startsWith(PREFIJO_CANAL)
                && sesiones.vigentesAnonimas(ahora, PREFIJO_USUARIO, PREFIJO_CANAL) >= MAXIMO_TOTAL) {
            throw new EscanerException.DemasiadasSolicitudes("Hay demasiadas conexiones con celulares abiertas. Intenta en unos minutos.");
        }
        SesionEscaner nueva = new SesionEscaner(generadorDeIds.get(), de, ahora);
        sesiones.guardar(nueva);
        return new SesionAbierta(nueva.id(), nueva.venceEn());
    }

    @Override
    public boolean enviar(String sesionId, String codigo) {
        return encontrar(sesionId).enviar(codigo, reloj.instant()).laPcLoEsperaba();
    }

    @Override
    public synchronized String idDelCanal(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new EscanerException.SesionNoDisponible();
        }
        Instant ahora = reloj.instant();
        String origen = PREFIJO_CANAL + usuario;
        return sesiones.buscarVigentePorOrigen(origen, ahora).map(SesionEscaner::id).orElseGet(() -> {
            SesionEscaner nueva = new SesionEscaner(generadorDeIds.get(), origen, ahora);
            sesiones.guardar(nueva);
            return nueva.id();
        });
    }

    @Override
    public EstadoDeSesion estado(String sesionId) {
        return encontrar(sesionId).estado(reloj.instant());
    }

    @Override
    public void latido(String sesionId) {
        encontrar(sesionId).latido(reloj.instant());
    }

    @Override
    public EsperaDeCodigos esperar(String sesionId, long desde) {
        return encontrar(sesionId).esperar(Math.max(0, desde), reloj.instant());
    }

    @Override
    public EsperaDeCodigos esperar(String sesionId, long desde, Acepta acepta) {
        return encontrar(sesionId).esperar(Math.max(0, desde), reloj.instant(), acepta == null ? Acepta.SOLO_CODIGOS : acepta);
    }

    @Override
    public boolean enviarFoto(String sesionId, String foto) {
        SesionEscaner sesion = encontrar(sesionId);
        if (!sesion.origen().startsWith(PREFIJO_CANAL)) {   // las conexiones por QR son públicas: no reciben fotos
            throw new EscanerException.FotoNoPermitida();
        }
        return sesion.enviarFoto(foto, reloj.instant());
    }

    @Override
    public LecturaDeCodigos leer(String sesionId, long desde) {
        return encontrar(sesionId).leer(Math.max(0, desde), reloj.instant());
    }

    @Override
    public LecturaDeCodigos leer(String sesionId, long desde, Acepta acepta) {
        return encontrar(sesionId).leer(Math.max(0, desde), reloj.instant(), acepta == null ? Acepta.SOLO_CODIGOS : acepta);
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
