package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.PublicacionWhatsappException;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaDeTextoWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ProductoParaPublicar;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PublicacionesAdminCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ProductoParaPublicarPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PublicacionesWhatsappPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: aplicacion] [Clean: Use Cases]
 *
 * <p>Orquesta: junta los datos del articulo, le pide al modelo que decida (las reglas viven en {@link PublicacionWhatsapp}) y guarda.
 */
@Service
@RequiredArgsConstructor
public class PublicacionesAdminService implements PublicacionesAdminCasoUso {

    /** La pantalla muestra lo mas reciente; las viejas ya no hacen falta ahi. */
    static final int MAX_LISTA = 200;

    private final PublicacionesWhatsappPort publicaciones;
    private final ProductoParaPublicarPort productos;

    @Override
    @Transactional
    public PublicacionWhatsapp crear(NuevaPublicacion nueva) {
        if (nueva.varianteId() == null) {
            throw PublicacionWhatsappException.invalida("Elige el artículo que se va a publicar");
        }
        ProductoParaPublicar producto = productos.de(nueva.varianteId())
                .orElseThrow(() -> PublicacionWhatsappException.invalida("No encontré ese artículo, o está dado de baja"));
        if (!producto.disponible()) {
            throw PublicacionWhatsappException.invalida("Ese artículo está agotado: no se publica para que nadie pida lo que no hay");
        }
        String cuerpo = nueva.texto() == null || nueva.texto().isBlank()
                ? PlantillaDeTextoWhatsapp.cuerpoDe(producto.nombre(), producto.precio())
                : nueva.texto();
        PublicacionWhatsapp p = PublicacionWhatsapp.nueva(nueva.varianteId(), nueva.grupo(), cuerpo, nueva.modoVenta(),
                producto.imagenPrincipalId(), nueva.programadaPara(), LocalDateTime.now());
        PublicacionWhatsapp conId = publicaciones.crear(p);
        conId.asignarIdentidad(conId.id());
        return publicaciones.guardar(conId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicacionWhatsapp> listar() {
        return publicaciones.listar(MAX_LISTA);
    }

    @Override
    @Transactional
    public PublicacionWhatsapp cancelar(int id) {
        PublicacionWhatsapp p = publicaciones.buscarConBloqueo(id).orElseThrow(() -> PublicacionWhatsappException.noSeEncuentra(id));
        p.cancelar();
        return publicaciones.guardar(p);
    }

    @Override
    @Transactional
    public PublicacionWhatsapp reintentar(int id) {
        PublicacionWhatsapp p = publicaciones.buscarConBloqueo(id).orElseThrow(() -> PublicacionWhatsappException.noSeEncuentra(id));
        p.reintentar();
        return publicaciones.guardar(p);
    }
}
