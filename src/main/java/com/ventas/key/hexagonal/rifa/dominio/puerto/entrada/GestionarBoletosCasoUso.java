package com.ventas.key.hexagonal.rifa.dominio.puerto.entrada;

import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.ModoDeCarga;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;

import java.util.List;

/**
 * Lo que se puede hacer con los boletos de una rifa desde la pantalla.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case]</p>
 */
public interface GestionarBoletosCasoUso {

    /** La pantalla: un renglon por (plataforma + perfil), con sus participaciones adentro. */
    List<GrupoDeBoletos> verAgrupados(Integer rifaId);

    /**
     * Alta de un perfil con varias participaciones de una sola pasada: la cabecera
     * (concursante, plataforma, perfil) se carga una vez y se crea una fila por URL
     * (reglas R1 y R3, decision D4).
     */
    GrupoDeBoletos cargar(CargaDeParticipaciones peticion);

    /** Suma una participacion a un grupo que ya existe, sin volver a cargar la cabecera. */
    GrupoDeBoletos agregarParticipacion(Integer rifaId, Plataforma plataforma, String urlPerfil,
            NuevaParticipacion participacion);

    /** Quita una participacion de un grupo. El grupo puede quedar vacio. */
    GrupoDeBoletos quitarParticipacion(Integer rifaId, Integer boletoId);

    /**
     * El alta completa.
     *
     * @param participaciones las URLs de lo que hizo el cliente. Cada una es un boleto.
     */
    record CargaDeParticipaciones(Integer rifaId, Integer concursanteId, Plataforma plataforma,
            String urlPerfil, List<NuevaParticipacion> participaciones) {
    }

    /**
     * Una URL de participacion y como validarla.
     *
     * @param modo {@code UNICA} rechaza la URL si ya existe; {@code REPETIDA_PERMITIDA} la
     *             acepta igual. Null se toma como {@code UNICA}.
     */
    record NuevaParticipacion(String urlParticipacion, String motivo, ModoDeCarga modo) {
    }
}
