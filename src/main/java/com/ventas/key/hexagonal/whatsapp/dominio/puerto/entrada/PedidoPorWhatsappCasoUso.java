package com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.SolicitudDePedido;

import java.time.Instant;
import java.util.Optional;

/**
 * Lo que el bot de WhatsApp hace con un pedido (reglas R16 a R21): validar lo que propone el modelo, mostrar el
 * resumen, esperar el «sí» del cliente y crear el Apartado. [Hexagonal: Driving Port] [Clean: Use Case Input]
 */
public interface PedidoPorWhatsappCasoUso {

    /**
     * Si hay un resumen esperando y el cliente contesta algo. «Sí» limpio escrito DESPUÉS de que el resumen salió → se
     * crea el pedido. Un «sí» escrito antes (o a la vez) de que el cliente pudiera leer el resumen se ignora sin
     * descartarlo. Un «sí» con pregunta («ok?») pide «responde solo SÍ». Cualquier otra cosa → el resumen se descarta y
     * se devuelve vacío (sigue el modelo).
     *
     * @param enviadoEn la hora que trae el mensaje del cliente (la de Meta); null = ahora
     */
    Optional<RespuestaDePedido> alResponder(long clienteId, String telefonoWhatsapp, String nombrePerfil,
                                            String mensajeDelCliente, Instant enviadoEn);

    /** El modelo propuso un pedido: se valida contra la base y se devuelve el resumen (o lo que falta). El resumen NO queda guardado todavía. */
    RespuestaDePedido proponer(long clienteId, String telefonoWhatsapp, SolicitudDePedido solicitud);

    /** El resumen salió de verdad hacia el cliente (estado ENVIADO): desde ahora puede confirmarlo. */
    void resumenEnviado(long clienteId, BorradorDePedido borrador, Instant enviadoEn);

    /** Se descarta el resumen pendiente (el bot se pausó, una persona tomó la conversación…). */
    void descartar(long clienteId);

    /** El cliente quiere cancelar un pedido: se anota el número y se avisa; el bot no cancela. */
    RespuestaDePedido solicitarCancelacion(String telefonoWhatsapp, String nombrePerfil, Integer numeroDePedido);

    /** El pedido llegó mal escrito en la marca: se le pide al cliente que repita los datos. */
    RespuestaDePedido marcaIlegible();

    /** Texto fijo para un «sí» que llegó cuando ya no hay resumen (venció o se perdió): nunca se deja al modelo decir que quedó hecho. */
    RespuestaDePedido resumenVencido();

    /** Texto con el número del cliente y lo que ya dio antes (enmascarado), para el modelo. */
    String contextoParaElModelo(String telefonoWhatsapp);
}
