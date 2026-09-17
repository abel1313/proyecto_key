package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * El catálogo que se le manda al modelo. Sin categoría detectada va en formato compacto para no
 * gastar tokens de más, y ahí es donde estaba el agujero.
 */
class ChatbotCatalogoContextoTest {

    private IVarianteRepository varianteRepository;
    private ChatbotChatVivoService servicio;

    @BeforeEach
    void setUp() {
        varianteRepository = mock(IVarianteRepository.class);
        IPalabraClaveRepository palabraClaveRepository = mock(IPalabraClaveRepository.class);
        when(palabraClaveRepository.findAll()).thenReturn(List.of());
        servicio = new ChatbotChatVivoService(varianteRepository, palabraClaveRepository);
    }

    // El caso real reportado (2026-09-17): un short cuyo modelo se llama "Surprise SU8183" y donde
    // la palabra "short" sólo vive en la presentación. El cliente preguntó por shorts y el bot
    // contestó "no tenemos" con el short en stock — porque el catálogo compacto omitía la
    // presentación y la línea no tenía la palabra "short" en ninguna parte.
    @Test
    void sinCategoriaElCatalogoCompactoIgualDiceQueEsElProducto() {
        when(varianteRepository.findByStockGreaterThanAndProductoHabilitado(anyInt(), anyChar(), any(Pageable.class)))
                .thenReturn(pagina(variante("Surprise SU8183", "short chico", "Surprise", 250.0, 1)));

        String contexto = servicio.obtenerContextoVariantes(null);

        assertThat(contexto).contains("short chico");
    }

    // La contraparte: talla y color siguen siendo sólo del formato detallado. Si se cuelan al
    // compacto, cada mensaje sin categoría paga tokens de más por hasta 1000 variantes.
    @Test
    void sinCategoriaElCatalogoCompactoNoMandaTallaNiColor() {
        Variantes v = variante("Surprise SU8183", "short chico", "Surprise", 250.0, 1);
        v.setTalla("19");
        v.setColor("Azul marino");
        when(varianteRepository.findByStockGreaterThanAndProductoHabilitado(anyInt(), anyChar(), any(Pageable.class)))
                .thenReturn(pagina(v));

        String contexto = servicio.obtenerContextoVariantes(null);

        assertThat(contexto).doesNotContain("talla:").doesNotContain("color:");
    }

    private Page<Variantes> pagina(Variantes... variantes) {
        return new PageImpl<>(List.of(variantes));
    }

    private Variantes variante(String nombreModelo, String presentacion, String marca,
                               Double precio, int stock) {
        Producto p = new Producto();
        p.setNombre(nombreModelo);
        p.setPrecioVenta(precio);

        Variantes v = new Variantes();
        v.setProducto(p);
        v.setPresentacion(presentacion);
        v.setMarca(marca);
        v.setStock(stock);
        return v;
    }
}
