package com.ventas.key.hexagonal.botredes.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ProductoDePublicacionPort;
import com.ventas.key.mis.productos.redessociales.IPublicacionSocialRepository;
import com.ventas.key.mis.productos.redessociales.PublicacionSocial;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * {@code publicacion_social.post_id_facebook} guarda el id que devolvió la red al publicar desde el
 * panel, sea de Facebook o de Instagram (el nombre de la columna es histórico).
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class ProductoDePublicacionJpaAdapter implements ProductoDePublicacionPort {

    private final IPublicacionSocialRepository publicaciones;

    public ProductoDePublicacionJpaAdapter(IPublicacionSocialRepository publicaciones) {
        this.publicaciones = publicaciones;
    }

    @Override
    public Optional<Integer> varianteDe(String publicacionId) {
        return publicaciones.findByPostIdFacebook(publicacionId)
                .map(PublicacionSocial::getVariante)
                .filter(Objects::nonNull)
                .map(v -> v.getId());
    }
}
