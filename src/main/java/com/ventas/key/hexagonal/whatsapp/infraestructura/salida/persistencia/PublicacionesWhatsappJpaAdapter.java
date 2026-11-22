package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoPublicacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ModoVenta;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PublicacionesWhatsappPort;
import com.ventas.key.mis.productos.redessociales.IPublicacionSocialRepository;
import com.ventas.key.mis.productos.redessociales.PublicacionSocial;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks — JPA]
 *
 * <p>Guarda en {@code publicacion_social} (plataforma 'whatsapp'). La entidad vive en {@code mis/productos/redessociales}
 * porque el escaneo de JPA solo cubre ese paquete.
 */
@Component
@RequiredArgsConstructor
public class PublicacionesWhatsappJpaAdapter implements PublicacionesWhatsappPort {

    private final IPublicacionSocialRepository repositorio;

    @Override
    public List<PublicacionWhatsapp> pendientesVencidasConBloqueo(LocalDateTime ahora, int maximo) {
        return repositorio.pendientesWhatsappConBloqueo(ahora, PageRequest.of(0, maximo)).stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<PublicacionWhatsapp> buscarConBloqueo(int id) {
        return repositorio.buscarWhatsappConBloqueo(id).map(this::aDominio);
    }

    @Override
    public Optional<PublicacionWhatsapp> buscar(int id) {
        return repositorio.buscarWhatsapp(id).map(this::aDominio);
    }

    @Override
    public PublicacionWhatsapp guardar(PublicacionWhatsapp p) {
        PublicacionSocial fila = repositorio.buscarWhatsapp(p.id()).orElseThrow();
        fila.setEstado(p.estado().name());
        fila.setIntentos(p.intentos());
        fila.setUltimoError(p.ultimoError());
        fila.setFechaPublicacion(p.fechaPublicacion());
        return aDominio(repositorio.save(fila));
    }

    private PublicacionWhatsapp aDominio(PublicacionSocial f) {
        return new PublicacionWhatsapp(f.getId(), f.getGrupoWhatsapp(), f.getDescripcionPublicada(), f.getCodigoRef(),
                ModoVenta.desde(f.getModoVenta()), f.getImagenId(), f.getScheduledPublishTime(),
                EstadoPublicacion.desde(f.getEstado()), f.getIntentos() == null ? 0 : f.getIntentos(), f.getUltimoError(),
                f.getFechaPublicacion());
    }
}
