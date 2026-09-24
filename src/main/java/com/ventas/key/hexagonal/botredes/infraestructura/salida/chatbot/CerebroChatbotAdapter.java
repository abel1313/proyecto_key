package com.ventas.key.hexagonal.botredes.infraestructura.salida.chatbot;

import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CerebroPort;
import com.ventas.key.mis.productos.chatbot.ChatbotBase;
import com.ventas.key.mis.productos.chatbot.ChatbotFacebookService;
import com.ventas.key.mis.productos.chatbot.ChatbotInstagramService;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * El chatbot de siempre (OpenAI), con el canal de cada red. Si falla o tarda, lanza la excepción y
 * el servicio escala.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class CerebroChatbotAdapter implements CerebroPort {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(25);

    private final ChatbotFacebookService facebook;
    private final ChatbotInstagramService instagram;
    private final IVarianteRepository variantes;

    public CerebroChatbotAdapter(ChatbotFacebookService facebook, ChatbotInstagramService instagram,
                                 IVarianteRepository variantes) {
        this.facebook = facebook;
        this.instagram = instagram;
        this.variantes = variantes;
    }

    @Override
    public String responderSobreProducto(RedSocial red, String texto, Integer varianteId, boolean esPrimeraVez) {
        Variantes variante = variantes.findById(varianteId).orElse(null);
        if (variante == null) {
            return responderComentarioSinProducto(red, texto, esPrimeraVez);
        }
        return canal(red).responderSobreProducto(texto, variante, esPrimeraVez).block(ESPERA_MAXIMA);
    }

    @Override
    public String responderComentarioSinProducto(RedSocial red, String texto, boolean esPrimeraVez) {
        return canal(red).responderComentarioSinProducto(texto, esPrimeraVez).block(ESPERA_MAXIMA);
    }

    @Override
    public String responderMensajeDirecto(RedSocial red, String texto, boolean esPrimeraVez) {
        return canal(red).responderMensajeDirecto(texto, esPrimeraVez).block(ESPERA_MAXIMA);
    }

    private ChatbotBase canal(RedSocial red) {
        return red == RedSocial.FACEBOOK ? facebook : instagram;
    }
}
