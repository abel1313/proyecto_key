package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ProductoParaPublicar;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ProductoParaPublicarPort;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks — JPA]
 *
 * <p>Lee el articulo (nombre del producto, precio normal, existencias) y su foto principal. El precio que se anuncia es el normal: una
 * rebaja sin activar no se anuncia (mismo criterio que el chatbot, {@code ChatbotBase.precioParaMostrar}).
 */
@Component
@RequiredArgsConstructor
public class ProductoParaPublicarJpaAdapter implements ProductoParaPublicarPort {

    private final IVarianteRepository variantes;
    private final IVarianteImagenRepository imagenes;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoParaPublicar> de(int varianteId) {
        return variantes.findById(varianteId)
                .filter(v -> v.getHabilitado() == '1')
                .map(v -> new ProductoParaPublicar(nombreDe(v), v.precioNormal(), v.getStock() > 0, imagenPrincipalDe(varianteId)));
    }

    private static String nombreDe(Variantes v) {
        return v.getProducto() == null ? null : v.getProducto().getNombre();
    }

    /** La consulta ya pone la principal primero. Sin foto: null y se publica solo el texto. */
    private Long imagenPrincipalDe(int varianteId) {
        List<Object[]> filas = imagenes.findImagenIdsConPrincipalByVarianteId(varianteId);
        return filas.isEmpty() || filas.get(0)[0] == null ? null : ((Number) filas.get(0)[0]).longValue();
    }
}
