package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Quién lo escribió: el cliente, el bot, una persona del negocio (desde la pantalla) o el sistema (avisos en el historial). */
public enum OrigenMensaje {
    CLIENTE,
    BOT,
    PERSONA,
    SISTEMA
}
