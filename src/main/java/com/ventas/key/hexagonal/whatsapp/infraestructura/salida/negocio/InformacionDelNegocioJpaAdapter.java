package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.negocio;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.InformacionDelNegocio;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.InformacionDelNegocioPort;
import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import com.ventas.key.mis.productos.repository.IConfiguracionNegocioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lee la dirección, el punto del mapa y las redes del negocio (regla R22). Es de SOLO LECTURA a propósito:
 * {@code NegocioService.obtenerConfig()} crea o borra filas de configuración al leer, y hacerlo en cada mensaje
 * podía duplicarlas; aquí solo se hace un {@code findAll} ordenado y, si no hay fila, se devuelve vacío.
 * Las redes se leen siempre (a diferencia de {@code getEstado()}, que las oculta cuando el negocio está abierto).
 * Se guarda un minuto, igual que el horario.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Slf4j
@Component
public class InformacionDelNegocioJpaAdapter implements InformacionDelNegocioPort {

    private static final long VIGENCIA_NANOS = 60_000_000_000L;

    private final IConfiguracionNegocioRepository repositorio;
    private volatile Optional<InformacionDelNegocio> guardado;
    private volatile long leidoEn;

    public InformacionDelNegocioJpaAdapter(IConfiguracionNegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<InformacionDelNegocio> actual() {
        Optional<InformacionDelNegocio> vigente = guardado;
        if (vigente != null && System.nanoTime() - leidoEn < VIGENCIA_NANOS) {
            return vigente;
        }
        try {
            Optional<InformacionDelNegocio> nuevo = leer();
            guardado = nuevo;
            leidoEn = System.nanoTime();
            return nuevo;
        } catch (RuntimeException e) {
            // Un fallo al leer no se guarda: se reintenta con el siguiente mensaje. El bot sigue sin esos datos.
            log.warn("WhatsApp: no se pudo leer la información del negocio: {}", e.toString());
            return Optional.empty();
        }
    }

    private Optional<InformacionDelNegocio> leer() {
        List<ConfiguracionNegocio> filas = repositorio.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        ConfiguracionNegocio c = filas.get(0);
        return Optional.of(new InformacionDelNegocio(c.getDireccion(), c.getLatitud(), c.getLongitud(),
                c.getWhatsappUrl(), c.getFacebookUrl(), c.getInstagramUrl(), c.getTiktokUrl()));
    }
}
