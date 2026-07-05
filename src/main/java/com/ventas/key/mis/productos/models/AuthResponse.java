package com.ventas.key.mis.productos.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private boolean debeCambiarPassword;

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public AuthResponse(String accessToken, boolean debeCambiarPassword) {
        this.accessToken = accessToken;
        this.debeCambiarPassword = debeCambiarPassword;
    }
}
