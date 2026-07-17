package com.ventas.key.mis.productos.Utils;

import com.ventas.key.mis.productos.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuthenticationUtils {

    private AuthenticationUtils(){
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    public static String jwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getCredentials().toString();
    }
    public static String jwtBearerToken() {
        return "Bearer ".concat(jwtToken());
    }

    public static boolean isAdminContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Usuario autenticado segun el JWT de la peticion actual (no lo que mande el body). */
    public static Usuario currentUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }

    /**
     * Igual que {@link #currentUsuario()} pero sin reventar en endpoints publicos (permitAll)
     * donde una peticion sin token deja un AnonymousAuthenticationToken con principal "anonymousUser"
     * (String, no Usuario) en el contexto -- ahi currentUsuario() lanzaria ClassCastException.
     */
    public static Optional<Usuario> currentUsuarioOpt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Usuario usuario)) {
            return Optional.empty();
        }
        return Optional.of(usuario);
    }
}
