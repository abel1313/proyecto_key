package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.entity.BaseId;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Fila unica (id=1 siempre) -- a diferencia del token de Facebook, el de TikTok expira cada
// ~24h y el refresh token rota en cada uso, asi que no puede vivir en un yml estatico: hace
// falta guardarlo donde el back lo pueda actualizar solo. Ver TikTokGraphClient.
@Entity
@Table(name = "tiktok_token")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TikTokToken extends BaseId {

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "open_id")
    private String openId;

    @Column(name = "access_token_expira_en")
    private LocalDateTime accessTokenExpiraEn;

    @Column(name = "refresh_token_expira_en")
    private LocalDateTime refreshTokenExpiraEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
