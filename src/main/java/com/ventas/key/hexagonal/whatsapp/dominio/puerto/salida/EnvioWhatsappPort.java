package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoEnvio;

/**
 * Manda mensajes por la API de WhatsApp de Meta ({@code POST /{phone-number-id}/messages}). No decide
 * si se puede enviar (eso es {@code PoliticaDeEnvio}) y nunca reintenta.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface EnvioWhatsappPort {

    ResultadoEnvio enviarTexto(String telefono, String texto);

    ResultadoEnvio enviarImagen(String telefono, String urlImagen, String pie);

    ResultadoEnvio enviarPlantilla(String telefono, PlantillaWhatsapp plantilla);
}
