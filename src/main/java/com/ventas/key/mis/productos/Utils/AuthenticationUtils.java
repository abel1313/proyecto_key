package com.ventas.key.mis.productos.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationUtils {

    public static String jwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getCredentials().toString();
    }
    public static String jwtBearerToken() {
        return "Bearer ".concat(jwtToken());
    }
}
