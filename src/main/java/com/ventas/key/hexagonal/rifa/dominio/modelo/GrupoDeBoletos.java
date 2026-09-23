package com.ventas.key.hexagonal.rifa.dominio.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un renglon de la pantalla: el perfil de un cliente en una red, con todas sus
 * participaciones adentro (regla R4).
 *
 * <p>Es lo que reemplaza a las filas sueltas. Antes dos participaciones del mismo perfil
 * se veian como "Facebook · juan · like" y aparte "Facebook · juan · compartio", cuando es
 * la misma persona en la misma red.</p>
 *
 * <p>[Hexagonal: Domain Model] [Clean: Entity]</p>
 */
public record GrupoDeBoletos(Integer concursanteId, String nombreConcursante, PerfilEnRed perfil,
        List<Participacion> participaciones) {

    public GrupoDeBoletos {
        participaciones = participaciones == null ? List.of() : List.copyOf(participaciones);
    }

    /**
     * Cuantos boletos suma este grupo. Es la cantidad de participaciones, no 1 por ser un
     * grupo: cada participacion es una fila y cada fila es una chance en el sorteo.
     */
    public int totalBoletos() {
        return participaciones.size();
    }

    public boolean estaVacio() {
        return participaciones.isEmpty();
    }

    /**
     * La fecha de la participacion mas nueva del grupo, o null si no tiene ninguna.
     *
     * <p>Existe para poder poner arriba lo que se toco recien. Con muchos concursantes
     * cargados, el orden de insercion deja el ultimo grupo hasta el final, que es
     * exactamente el problema de tener que bajar hasta abajo y volver a subir.</p>
     */
    public LocalDate ultimaParticipacion() {
        return participaciones.stream()
                .map(Participacion::fecha)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /** La participacion cuya URL coincide, si ya esta cargada en este grupo. */
    public Participacion buscarPorUrl(String url) {
        String clave = PerfilEnRed.normalizar(url);
        return participaciones.stream()
                .filter(p -> p.claveUrl().equals(clave))
                .findFirst()
                .orElse(null);
    }

    public boolean yaTieneLaUrl(String url) {
        return buscarPorUrl(url) != null;
    }

    /** El mismo grupo sin ese boleto: para validar una edicion contra los demas. */
    public GrupoDeBoletos sin(Integer boletoId) {
        return new GrupoDeBoletos(concursanteId, nombreConcursante, perfil,
                participaciones.stream().filter(p -> !java.util.Objects.equals(p.boletoId(), boletoId)).toList());
    }

    public GrupoDeBoletos con(Participacion nueva) {
        List<Participacion> juntas = new ArrayList<>(participaciones);
        juntas.add(nueva);
        return new GrupoDeBoletos(concursanteId, nombreConcursante, perfil, juntas);
    }
}
