package com.ventas.key.hexagonal.campana.dominio.puerto.salida;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;

import com.ventas.key.hexagonal.campana.dominio.modelo.ResumenDePersona;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Port] Las personas con código de campaña y lo que han comprado en cada campaña. */
public interface PersonasCampanaPort {

    /** {@code false} si las tablas todavía no existen (falta la migración): la campaña no participa de las ventas. */
    boolean disponible();

    Optional<PersonaCampana> porUuid(String uuid);

    /** Busca por correo o por teléfono (últimos 10 dígitos), para no crear dos códigos a la misma persona. */
    Optional<PersonaCampana> porContacto(String correo, String telefono10);

    PersonaCampana guardar(PersonaCampana persona);

    /** Lo que esta persona suma hacia la meta de esa campaña (compras ligadas, pagadas, que no estén canceladas ni pendientes). */
    BigDecimal acumulado(int campanaId, int personaId);

    /**
     * Lo PAGADO por esta persona entre la fecha de inicio y la de fin de la campaña (contado y abonos; todo suma, juguetes incluidos): sus pedidos
     * ligados más los de su cliente. Por defecto, lo ligado; el adaptador de base de datos lo calcula por fechas.
     */
    default BigDecimal acumulado(Campana campana, PersonaCampana persona) {
        return acumulado(campana.id(), persona.id());
    }

    /** Rellena a qué cliente pertenece la persona si todavía no lo sabe. Por defecto no hace nada. */
    default void completarCliente(int personaId, Integer clienteId, Integer clienteSinRegistroId) {
    }

    /** Liga un pedido a la persona dentro de la campaña, con lo que suma hacia la meta (0 si todavía no está pagado). */
    void ligarPedido(int campanaId, int personaId, int pedidoId, BigDecimal monto);

    /** La persona a la que se ligó este pedido, si alguna (para reenviarle su código). */
    Optional<PersonaCampana> dePedido(int pedidoId);

    /** Cada persona con lo que lleva en esta campaña, para el reporte. */
    List<ResumenDePersona> resumen(int campanaId);

    /** Borra a la persona y todo lo que la liga a pedidos. Los pedidos no se tocan. */
    void eliminar(int personaId);
}
