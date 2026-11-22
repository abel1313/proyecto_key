package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Lo que debe hacer el bot con un mensaje: IGNORAR (no contestar nada), RESPONDER, ESCALADO (el back ya aviso al admin; el bot solo manda el
 * texto de cortesia) u OFRECER_PRIVADO (contestar y mandar a la persona al chat privado).
 */
public enum AccionBot {
    RESPONDER, IGNORAR, ESCALADO, OFRECER_PRIVADO
}
