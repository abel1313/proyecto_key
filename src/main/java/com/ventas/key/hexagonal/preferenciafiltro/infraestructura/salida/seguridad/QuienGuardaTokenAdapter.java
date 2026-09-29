package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.salida.seguridad;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.QuienGuarda;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.QuienGuardaPort;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers — Spring Security]
 *
 * <p>Cada usuario tiene un solo rol. {@code ROLE_USUARIO} es el que recibe quien se registra en la
 * tienda (RegistroService), así que ese es el cliente; cualquier otro rol es personal.
 */
@Component
public class QuienGuardaTokenAdapter implements QuienGuardaPort {

    private static final String ROL_CLIENTE = "ROLE_USUARIO";

    @Override
    public QuienGuarda actual() {
        Usuario usuario = AuthenticationUtils.currentUsuario();
        String rol = usuario.getRoles() != null ? usuario.getRoles().getNombreRol() : null;
        return new QuienGuarda(usuario.getId(), rol != null && !ROL_CLIENTE.equals(rol));
    }
}
