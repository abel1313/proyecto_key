package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoPublicacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ModoVenta;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PublicacionesWhatsappPort;
import com.ventas.key.mis.productos.redessociales.IPublicacionSocialRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
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
    private final IVarianteRepository variantes;

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
        fila.setDescripcionPublicada(p.texto());
        fila.setCodigoRef(p.codigoRef());
        fila.setEstado(p.estado().name());
        fila.setIntentos(p.intentos());
        fila.setUltimoError(p.ultimoError());
        fila.setFechaPublicacion(p.fechaPublicacion());
        return aDominio(repositorio.save(fila));
    }

    @Override
    public PublicacionWhatsapp crear(PublicacionWhatsapp p) {
        PublicacionSocial fila = new PublicacionSocial();
        fila.setVariante(variantes.getReferenceById(p.varianteId()));
        fila.setPlataforma("whatsapp");
        fila.setTipoPublicacion(p.imagenId() == null ? "texto" : "foto");
        fila.setDescripcionPublicada(p.texto());
        fila.setImagenId(p.imagenId());
        fila.setScheduledPublishTime(p.programadaPara());
        fila.setEstado(p.estado().name());
        fila.setIntentos(p.intentos());
        fila.setGrupoWhatsapp(p.grupo());
        fila.setModoVenta(p.modoVenta().name());
        return aDominio(repositorio.save(fila));
    }

    @Override
    public Optional<PublicacionWhatsapp> buscarPorCodigo(String codigoRef) {
        return repositorio.buscarWhatsappPorCodigo(codigoRef).map(this::aDominio);
    }

    @Override
    public List<PublicacionWhatsapp> publicadasDelGrupoDesde(String grupo, LocalDateTime desde) {
        return repositorio.publicadasWhatsappDelGrupoDesde(grupo, desde).stream().map(this::aDominio).toList();
    }

    @Override
    public List<PublicacionWhatsapp> listar(int maximo) {
        return repositorio.listarWhatsapp(PageRequest.of(0, maximo)).stream().map(this::aDominio).toList();
    }

    private PublicacionWhatsapp aDominio(PublicacionSocial f) {
        return new PublicacionWhatsapp(f.getId(), f.getVariante() == null ? null : f.getVariante().getId(), f.getGrupoWhatsapp(), f.getDescripcionPublicada(), f.getCodigoRef(),
                ModoVenta.desde(f.getModoVenta()), f.getImagenId(), f.getScheduledPublishTime(),
                EstadoPublicacion.desde(f.getEstado()), f.getIntentos() == null ? 0 : f.getIntentos(), f.getUltimoError(),
                f.getFechaPublicacion());
    }
}
