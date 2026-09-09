package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

// Canal: comentarios de Facebook (ver FacebookCommentBotService). No sobreescribe promptBase()
// -- usa el mismo texto por defecto de ChatbotBase mientras nadie pida uno propio solo para
// Facebook. Si algun dia hace falta un ajuste que NO deba aplicar a Instagram/sitio web, se
// sobreescribe promptBase() aqui, sin tocar las otras clases.
@Service
public class ChatbotFacebookService extends ChatbotBase {

    public ChatbotFacebookService(IVarianteRepository varianteRepository,
                                   IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    @Override
    public Mono<String> responderComentario(String comentario, Variantes varianteDelPost, boolean esPrimeraVez) {
        return super.responderComentario(comentario, varianteDelPost, esPrimeraVez);
    }
}
