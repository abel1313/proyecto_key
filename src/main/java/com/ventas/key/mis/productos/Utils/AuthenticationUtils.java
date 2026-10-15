package com.ventas.key.mis.productos.Utils;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;
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

    /**
     * Si quien hace la peticion es admin o tiene la accion puntual {@code clave} en la pantalla
     * {@code ruta} (Gestion de roles). Misma autoridad que arma SecurityConfig.accion(). Sin sesion
     * (endpoint publico) responde false.
     */
    public static boolean tieneAccion(String ruta, String clave) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String autoridad = JwtAuthenticationFilter.PREFIJO_AUTORIDAD_PANTALLA + ruta
                + JwtAuthenticationFilter.SUFIJO_AUTORIDAD_ACCION + clave;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals(autoridad));
    }

    /**
     * Agregar articulo (tienda/venta) con el permiso "ver-todos-los-modelos" (2026-10-08): el buscador
     * de modelos trae tambien los que no tienen stock, los deshabilitados y los dados de baja. El
     * admin siempre los ve. {@code pedido} es lo que pidio la pantalla; sin permiso se ignora.
     */
    public static boolean puedeVerTodosLosModelos(boolean pedido) {
        return isAdminContext() || (pedido && tieneAccion("tienda/venta", "ver-todos-los-modelos"));
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
