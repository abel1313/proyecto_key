package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnviarCodigoVerificacionUsuarioRequest {

    @NotBlank(message = "El nombre de usuario o correo es obligatorio")
    private String userName;

    /**
     * true = el usuario pidió explícitamente "reenviar código" -> siempre manda uno nuevo.
     * false/ausente = envío automático (al cargar la pantalla, al fallar un login sin verificar,
     * al abrir el modal de "Verificar correo" del admin) -> si ya hay uno vigente sin usar, se
     * reutiliza y NO se manda otro correo, para no invalidar en silencio uno que el usuario
     * todavía no alcanza a leer.
     */
    private boolean forzarNuevo;
}
