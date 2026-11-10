package com.ventas.key.hexagonal.campana.dominio.puerto.salida;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;

import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Port] Lo que el dominio necesita para guardar y leer campañas. */
public interface CampanasPort {

    List<Campana> listar();

    Optional<Campana> buscar(int id);

    /** Las campañas encendidas (solo debería haber una; las fechas las revisa el dominio). Vacío si las tablas todavía no existen. */
    List<Campana> activas();

    /** Crea (id {@code null}) o actualiza. Rechaza un producto que no existe. */
    Campana guardar(Campana campana);

    void eliminar(int id);

    /** ¿La campaña ya tiene compras ligadas (pedidos de personas con código)? Entonces no se borra: se apaga. Por defecto no. */
    default boolean tieneComprasLigadas(int id) {
        return false;
    }

    /** Enciende esta campaña y apaga las demás (solo una activa a la vez). */
    void activarSola(int id);

    void apagar(int id);
}
