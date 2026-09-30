package com.ventas.key.mis.productos.redessociales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class TikTokToken {

    // Id ASIGNADO (siempre 1), no autogenerado: la columna es `id INT PRIMARY KEY` sin
    // AUTO_INCREMENT. Antes heredaba BaseId (@GeneratedValue IDENTITY) y Hibernate armaba el
    // INSERT sin el id -> "Field 'id' doesn't have a default value" al conectar la cuenta en
    // prod (2026-10-01). Aunque la tabla tuviera AUTO_INCREMENT, tras desconectar y volver a
    // conectar la fila nueva saldria con id=2 y findById(1) ya no la encontraria.
    @Id
    @Column(name = "id")
    private Integer id;

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
