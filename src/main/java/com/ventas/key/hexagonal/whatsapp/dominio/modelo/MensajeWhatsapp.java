package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Lo que el bot vio en WhatsApp. {@code chat} es el nombre del grupo (o de la persona en privado); {@code ref} y {@code citaTexto} son
 * opcionales: el codigo (J42) que el bot ya saco del mensaje y el texto del mensaje citado. {@code mencion}: mencionaron al bot.
 */
public record MensajeWhatsapp(CanalWhatsapp canal, String chat, String remitente, String texto, String ref, String citaTexto,
                              boolean mencion) {

    public boolean esGrupo() {
        return canal == CanalWhatsapp.GRUPO;
    }
}
