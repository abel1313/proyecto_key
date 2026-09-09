package com.ventas.key.mis.productos.redessociales;

import lombok.Getter;
import lombok.Setter;

// Paso unico, manual: el admin visita la URL de autorizacion de TikTok en el navegador
// (ver TIKTOK_SETUP.md paso 4), TikTok lo redirige a redirectUri con ?code=... en la URL, y ese
// code se manda aqui junto con la misma redirectUri para completar el intercambio.
@Getter
@Setter
public class AutorizarTikTokRequest {
    private String code;
    private String redirectUri;
}
