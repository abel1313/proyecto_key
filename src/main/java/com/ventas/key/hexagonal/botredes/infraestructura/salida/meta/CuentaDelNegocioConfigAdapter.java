package com.ventas.key.hexagonal.botredes.infraestructura.salida.meta;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CuentaDelNegocioPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * La página de Facebook y la cuenta de Instagram del negocio, según la configuración del ambiente.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class CuentaDelNegocioConfigAdapter implements CuentaDelNegocioPort {

    private final String pageId;
    private final String igUserId;

    public CuentaDelNegocioConfigAdapter(@Value("${facebook.page-id:}") String pageId,
                                         @Value("${instagram.account-id:}") String igUserId) {
        this.pageId = pageId;
        this.igUserId = igUserId;
    }

    @Override
    public boolean esDelNegocio(RedSocial red, String autorId) {
        if (autorId == null || autorId.isBlank()) {
            return false;
        }
        return autorId.equals(red == RedSocial.FACEBOOK ? pageId : igUserId);
    }
}
