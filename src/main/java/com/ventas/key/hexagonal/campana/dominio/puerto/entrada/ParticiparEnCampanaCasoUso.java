package com.ventas.key.hexagonal.campana.dominio.puerto.entrada;

import com.ventas.key.hexagonal.campana.dominio.modelo.ClienteDeCodigo;
import com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente;
import com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.campana.dominio.modelo.RegistroDeCompra;
import com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.VerificacionDeCodigo;

import java.util.Optional;

/** [Hexagonal: Driving Port] Lo que la venta y Venta directa hacen con la campaña vigente. */
public interface ParticiparEnCampanaCasoUso {

    /** Decide los precios de campaña de una venta. No guarda nada: sin campaña vigente (o sin las tablas) devuelve {@link DecisionDeCampana#NINGUNA}. */
    DecisionDeCampana decidir(SolicitudDeCampana solicitud);

    /** Al cerrar la venta: guarda a la persona nueva, liga el pedido y le manda su código por correo. Nunca hace caer la venta. */
    default void registrarCompra(DecisionDeCampana decision, int pedidoId, FormaDeCobro cobro) {
        registrarCompra(decision, pedidoId, cobro, true);
    }

    /**
     * Igual que el anterior, pero con {@code mandarCorreo=false} no manda el correo del código (quien llama lo incluye en otro correo, p. ej. el de
     * «Agrega tu compra a tu cuenta») y devuelve a quién se ligó la compra ({@link Optional#empty()} si no hubo campaña o persona).
     */
    Optional<RegistroDeCompra> registrarCompra(DecisionDeCampana decision, int pedidoId, FormaDeCobro cobro, boolean mandarCorreo);

    /** El botón «Verificar» de Venta directa. {@code quien} limita los intentos (usuario que pregunta). */
    VerificacionDeCodigo verificar(String quien, String codigo, ContactoCliente contacto);

    /**
     * Lo que lleva pagado en la campaña vigente un cliente reconocido por su correo, su teléfono o su cliente (registrado o sin registro), SIN código.
     * Sirve para que Venta directa calcule en vivo si ya llega a la meta; el back vuelve a decidir al cobrar. {@code NO_EXISTE} si no se le reconoce.
     */
    VerificacionDeCodigo acumuladoDelCliente(String quien, ContactoCliente contacto);

    /** Escanear el QR: devuelve a quién pertenece el código (para llenar los datos del cliente) y cuánto lleva. {@code quien} limita los intentos. */
    ClienteDeCodigo buscarPorCodigo(String quien, String codigo);

    void eliminarPersona(int personaId);

    /** ¿Este pedido está ligado a una persona con código? (el botón «Enviar código de campaña» de Mis pedidos). */
    boolean pedidoTieneCodigo(int pedidoId);

    /** Vuelve a mandar por correo el código de la persona de este pedido. */
    ResultadoReenvio reenviarCodigo(int pedidoId);

    enum ResultadoReenvio { ENVIADO, SIN_CODIGO, SIN_CORREO }
}
