package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.config;

import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ContactoNegocioPort;
import com.ventas.key.mis.productos.repository.IConfiguracionNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Gateway]
 *
 * <p>El enlace de WhatsApp que el dueño configuro en el negocio ({@code configuracion_negocio.whatsapp_url}, el mismo de
 * {@code GET /v1/negocio/contactos}). Se toma la fila mas antigua, igual que {@code NegocioService}.
 */
@Component
@RequiredArgsConstructor
public class ContactoNegocioConfigAdapter implements ContactoNegocioPort {

    private final IConfiguracionNegocioRepository configuracion;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> whatsappUrl() {
        return configuracion.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .findFirst()
                .map(c -> c.getWhatsappUrl())
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim);
    }
}
