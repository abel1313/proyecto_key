package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.PublicacionWhatsappException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PublicacionesParaElBotCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ImagenDePublicacionPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PublicacionesWhatsappPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: aplicacion] [Clean: Use Cases]
 *
 * <p>Orquesta: lee por el puerto, le pide al modelo que decida (las reglas viven en {@link PublicacionWhatsapp}) y guarda.
 */
@Service
@RequiredArgsConstructor
public class PublicacionesParaElBotService implements PublicacionesParaElBotCasoUso {

    /** Tope por llamada: el bot publica una por una y asi nunca se queda con muchas "en proceso". */
    static final int MAX_POR_LLAMADA = 5;

    private final PublicacionesWhatsappPort publicaciones;
    private final ImagenDePublicacionPort imagenes;

    @Override
    @Transactional
    public List<PublicacionWhatsapp> recogerPendientes() {
        List<PublicacionWhatsapp> lista = publicaciones.pendientesVencidasConBloqueo(LocalDateTime.now(), MAX_POR_LLAMADA);
        for (PublicacionWhatsapp p : lista) {
            p.tomarParaPublicar();
            publicaciones.guardar(p);
        }
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] imagenDe(int publicacionId) {
        PublicacionWhatsapp p = publicaciones.buscar(publicacionId)
                .orElseThrow(() -> PublicacionWhatsappException.noSeEncuentra(publicacionId));
        if (p.imagenId() == null) {
            throw PublicacionWhatsappException.sinImagen(publicacionId);
        }
        return imagenes.jpegLiviano(p.imagenId()).orElseThrow(() -> PublicacionWhatsappException.sinImagen(publicacionId));
    }

    @Override
    @Transactional
    public void marcarPublicada(int publicacionId) {
        PublicacionWhatsapp p = publicaciones.buscarConBloqueo(publicacionId)
                .orElseThrow(() -> PublicacionWhatsappException.noSeEncuentra(publicacionId));
        p.marcarPublicada(LocalDateTime.now());
        publicaciones.guardar(p);
    }

    @Override
    @Transactional
    public void marcarError(int publicacionId, String detalle) {
        PublicacionWhatsapp p = publicaciones.buscarConBloqueo(publicacionId)
                .orElseThrow(() -> PublicacionWhatsappException.noSeEncuentra(publicacionId));
        p.registrarError(detalle);
        publicaciones.guardar(p);
    }
}
