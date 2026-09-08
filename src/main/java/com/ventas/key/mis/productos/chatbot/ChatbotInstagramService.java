package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

// Canal: Instagram -- comentarios (InstagramCommentBotService) y mensajes directos
// (InstagramDirectMessageBotService), ambos del mismo canal/plataforma. No sobreescribe
// promptBase() -- usa el mismo texto por defecto de ChatbotBase mientras nadie pida uno propio
// solo para Instagram. Si algun dia hace falta un ajuste que NO deba aplicar a
// Facebook/sitio web, se sobreescribe promptBase() aqui, sin tocar las otras clases.
@Service
public class ChatbotInstagramService extends ChatbotBase {

    public ChatbotInstagramService(IVarianteRepository varianteRepository,
                                    IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    @Override
    public Mono<String> responderComentario(String comentario, Variantes varianteDelPost, boolean esPrimeraVez) {
        return super.responderComentario(comentario, varianteDelPost, esPrimeraVez);
    }
}
