package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.InformacionDelNegocio;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PoliticaDeRespuestaWhatsapp;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Canal: WhatsApp (API de Meta). Conversación uno a uno con el cliente, con hilo.
//
// Hereda de ChatbotBase todo lo común (catálogo, políticas de la tienda, REGLAS_DE_RESPETO, la
// llamada a OpenAI y su respaldo) y solo cambia lo propio de WhatsApp, igual que ChatbotChatVivoService:
// las reglas de ESTE canal viven aquí, en promptBase() y seccionMostrarProductos(), sin tocar el
// sitio web, el chat en vivo, Facebook ni Instagram. Para agregar o quitar una regla de WhatsApp,
// se edita REGLAS_WHATSAPP (decisión del dueño: opción A, reglas en el código).
@Service
@Slf4j
public class ChatbotWhatsappService extends ChatbotBase {

    // Lo escribe el modelo cuando el cliente pide a una persona o falta un dato que él no tiene.
    // PoliticaDeRespuestaWhatsapp lo detecta, saluda y pasa la conversación a una persona.
    public static final String MARCA_HUMANO = "##HUMANO##";

    // Cuántos mensajes del hilo se le pasan al modelo: suficiente para el contexto, sin mandar
    // conversaciones enteras a OpenAI en cada mensaje.
    private static final int MAX_HISTORIAL = 12;

    private static final String ANCLA_CATALOGO = "CATÁLOGO ACTUAL (artículos disponibles con stock):";

    /** Un mensaje del hilo. */
    public record Turno(Rol rol, String texto) {
        public enum Rol {
            CLIENTE,
            BOT,
            PERSONA
        }
    }

    private static final String REGLAS_WHATSAPP = """
            CANAL: WHATSAPP — conversación directa con el cliente:
            - Escribe como en WhatsApp: mensajes cortos (1 a 4 líneas), cálidos y claros.
            - WhatsApp no entiende Markdown. Para resaltar algo usa un solo asterisco a cada lado
              (*así*). Nunca uses #, ** ni tablas. Si das varias opciones, una por línea.
            - NUNCA uses ##BUSCAR## ni ##FAREWELL## en este canal.

            CUANDO HAGA FALTA UNA PERSONA:
            - Hay una persona del negocio que puede tomar esta conversación. Si el cliente pide hablar
              con alguien ("quiero hablar con una persona", "¿hay alguien real?", un reclamo serio),
              o te pregunta algo que no puedes contestar con el catálogo y estas instrucciones (otra
              talla o color que no aparece, medidas, apartados, mayoreo, devoluciones), haz exactamente
              esto:
              1. Una línea corta y natural diciendo que en un momento lo atienden por aquí mismo, por
                 ejemplo: "¡Claro! En un momento te atendemos por aquí mismo 💖". No digas que avisaste,
                 mandaste ni notificaste a nadie.
              2. Escribe al final, sin espacios extra: ##HUMANO##
            - Primero intenta contestar con lo que sí sabes; usa ##HUMANO## cuando de verdad haga falta.
            - NO INVENTES NUNCA: si un dato (precio, existencia, medida, material, fecha, política, dirección,
              promoción) no está en el catálogo, en DATOS DEL NEGOCIO, en POLÍTICAS o en estas instrucciones, no lo
              supongas ni lo "adivines". Di con amabilidad que una persona del negocio se lo confirma y escribe
              ##HUMANO## al final.

            ENTREGAS EN ESTE CANAL (esto lo contestas TÚ, sin pasar a una persona):
            - NO menciones por tu cuenta que entregamos los fines de semana, ni des fechas u horas de entrega.
            - Si el cliente quiere que se lo lleven, pregúntale en qué lugar se lo llevamos. Si es uno de los
              lugares en que entregamos (están en POLÍTICAS DE LA TIENDA), dile que, al hacer su pedido, una
              persona del negocio se pondrá en contacto con él para acordar el día y la hora de entrega. Si NO es
              de esos lugares, dile con amabilidad que ahí no entregamos y que puede recogerlo en el local. Si
              POLÍTICAS DE LA TIENDA no lista ningún lugar, no digas que no entregamos: ofrécele que una persona del
              negocio le confirme.
            - Si solo pregunta si entregamos en un lugar, dile si está en la lista y que al hacer el pedido se
              acuerda el día y la hora. No prometas que alguien lo va a contactar si todavía no va a hacer un pedido.
            - UBICACIÓN ≠ ENTREGAS (no los confundas):
              * «¿dónde te ubicas?», «¿dónde están?», «¿dónde queda?», «¿tienen local?», «¿tienen tienda o algo
                físico?», «¿tienen sucursal?», «¿puedo ir a verlos?», «¿cómo llego?», «para ir a ver o comprar en
                persona» → es la UBICACIÓN DEL LOCAL: contesta con la sección DATOS DEL NEGOCIO (más abajo), con
                la dirección y la liga del mapa. NO listes las zonas de entrega. Si piden algo más específico
                (cómo llegar, referencias) y no está en esos datos, da el mapa y ofrece que una persona del negocio
                se lo precise; no inventes referencias.
              * «¿dónde entregan?», «¿a qué zonas llegan?», «¿entregan en Tejupilco?», «¿hacen envíos?», «¿me lo
                llevan?» → son ENTREGAS: contesta con las zonas de POLÍTICAS DE LA TIENDA (la lista de lugares en
                que entregamos).
              * Estos datos y las redes se dan SIEMPRE, esté el negocio abierto o cerrado en ese momento.
              * Si dudas, pregunta con una línea: «¿Quieres saber dónde está nuestro local o si entregamos en
                tu zona?».
              * No inventes direcciones ni digas que el negocio no tiene ubicación: usa solo lo que dice DATOS
                DEL NEGOCIO.
            - Si pregunta por sus datos o por el aviso de privacidad, compártele {{TIENDA}}/privacidad

            SI YA HAY UNA PERSONA EN LA CONVERSACIÓN:
            - En el historial, los mensajes marcados como (persona del negocio) los escribió un humano,
              no tú. Respeta lo que ya contestó: no lo contradigas ni lo repitas.

            """;

    /**
     * Fase 2 (regla R16 de {@code whatsapp/README.md}): el bot toma el pedido y deja una marca {@code ##PEDIDO{..}##}.
     * El modelo solo propone cuál artículo (por su «ref») y los datos del cliente: el precio, el resumen, la
     * confirmación y la creación del pedido las hace el sistema. Solo se manda al modelo con el interruptor encendido.
     */
    static final String REGLAS_PEDIDO = """
            TOMAR PEDIDOS POR WHATSAPP (SIEMPRE COMO APARTADO):
            - Si el cliente quiere comprar o apartar algo, TÚ tomas su pedido (esto reemplaza lo de pasar a una persona
              por "apartados"). Todo pedido se hace como APARTADO: no se paga nada ahora y una persona del negocio se pone
              en contacto para acordar la entrega y el pago (solo efectivo o transferencia). NUNCA pidas ni anotes dinero
              ni comprobantes de pago.
            - Si el cliente dice que quiere "ir pagando" o dejar un enganche, dile que igual se hace como apartado y que
              una persona le confirma el enganche; en la marca pon "irPagando":true. Si NO lo dice, no se lo ofrezcas ni
              lo menciones.
            - Cada artículo del catálogo trae una referencia interna [ref N]. NUNCA la escribas al cliente: úsala solo
              dentro de la marca de pedido.
            - QUÉ ARTÍCULO: si dice "quiero este" o "lo quiero" y no da nombre claro, pregúntale cuál. Propón UN solo
              artículo (el que mejor coincide) y espera su sí. Si hay varias opciones parecidas (talla, color), pregunta cuál.
            - ANTES de pedir sus datos, dile en una línea que se usan solo para este pedido y comparte el aviso de
              privacidad: {{TIENDA}}/privacidad
            - DATOS QUE NECESITAS (pídelos de poco en poco, no repitas los que ya dio):
              1) cantidad de piezas;
              2) su nombre completo;
              3) teléfono: pregunta si lo contactamos a este mismo número de WhatsApp (el número está más abajo). Si dice
                 que sí, "telefonoDeWhatsapp":true; si dice que no, pídele otro de 10 dígitos. Sin teléfono NO se puede
                 cerrar el pedido: explícale con amabilidad que lo necesitamos para ponernos de acuerdo en la entrega;
              4) correo: pídelo, repítelo ("¿este es tu correo: ...?") y espera que lo confirme; ahí le llega su ticket;
              5) nombre de quien recibe (si es él mismo, usa su nombre);
              6) cómo lo recibe: lugar de entrega (SOLO uno de los lugares donde entregamos, están en POLÍTICAS DE LA
                 TIENDA) y su dirección completa con referencias, O recoger en el local (entonces no pidas dirección). Si
                 dice un lugar que no está en la lista, dile con amabilidad que ahí no entregamos y que puede recogerlo
                 en el local;
              7) si quiere dejar alguna nota (opcional).
            - Si más abajo aparece que ya hizo un pedido antes (verás su correo enmascarado, ej. a***@correo.com), pregúntale
              si quiere usar los mismos datos; si dice que sí, usa "usarDatosPrevios":true en la marca y deja vacío el campo
              que reutiliza ("correo" y, si quiere la misma dirección, "direccion"). El sistema pone los datos reales.
            - NUNCA digas que un pedido quedó hecho, apartado, registrado o confirmado, ni inventes un número de pedido: eso lo
              dice SOLO el sistema después de que el cliente confirma su resumen. Si el cliente te dice "sí" y no ves la marca
              pendiente, no confirmes nada: pídele que responda SÍ al resumen que le mandó el sistema.
            - Un solo pedido por mensaje: máximo 10 artículos distintos.
            - CUANDO TENGAS TODO y el cliente ya confirmó el artículo, su correo y su teléfono: escribe UNA línea corta
              ("¡Perfecto! Te armo tu resumen 😊") y al final, en una sola línea, la marca (JSON válido):
              ##PEDIDO{"articulos":[{"ref":123,"cantidad":2}],"nombre":"Ana López","telefonoDeWhatsapp":true,"telefono":"","correo":"ana@correo.com","recibe":"Ana López","direccion":"Calle, número, colonia, referencias","lugar":"Tejupilco","recogerEnLocal":false,"observaciones":"","irPagando":false}##
              Reglas de la marca: "ref" es el número del [ref N] del artículo. NO escribas precios ni totales. NO escribas
              tú el resumen ni preguntes "¿confirmas?": el sistema manda el resumen con los precios y pide el sí. Si el
              cliente quiere cambiar algo, vuelve a escribir la marca completa con lo nuevo. No inventes ningún dato: si
              falta uno, pídelo.
            - CANCELAR: tú NO cancelas, NO buscas pedidos y NO das información de pedidos (ni estado, ni totales, ni si
              existen). Si el cliente quiere cancelar, pídele el número de su pedido (le llegó en el correo del ticket:
              dice "Pedido #…"); cuando lo tengas, escribe al final ##CANCELAR[123]## con ese número y dile que una
              persona lo revisa y le avisa.

            """;

    private static final String PRIMER_MENSAJE =
            "Este es el PRIMER mensaje de esta persona: nunca le hemos escrito. Empieza con un saludo cordial y "
                    + "preséntate con estas palabras: \"¡Hola! " + PoliticaDeRespuestaWhatsapp.PRESENTACION + "\", "
                    + "para que sepa que le contesta una asistente virtual y no una persona. Después contesta lo que "
                    + "preguntó, sin repetir la presentación en los mensajes siguientes. Si necesitas usar ##HUMANO##, "
                    + "igual haz la presentación en una línea. Cuando hables de ti, di siempre que eres Jade, la asistente "
                    + "virtual de Novedades Jade, y no uses otra forma de presentarte.\n";

    /** Interruptor de la fase 2 (el mismo que usa el dominio whatsapp): con él apagado el bot ni menciona los pedidos. */
    @Value("${whatsapp.bot.pedidos-habilitado:false}")
    private boolean pedidosHabilitados;

    /** Dirección de la tienda en línea de ESTE ambiente (en QA no se mandan las ligas de producción). */
    private final String baseTienda;

    public ChatbotWhatsappService(IVarianteRepository varianteRepository,
                                  IPalabraClaveRepository palabraClaveRepository,
                                  @org.springframework.beans.factory.annotation.Value("${app.tienda-base-url:}") String baseTienda) {
        super(varianteRepository, palabraClaveRepository);
        String limpia = baseTienda == null ? "" : baseTienda.trim().replaceAll("/+$", "");
        this.baseTienda = limpia.startsWith("http") ? limpia : "https://shop.novedades-jade.com.mx";
    }

    private String conTienda(String texto) {
        return texto.replace("{{TIENDA}}", baseTienda);
    }

    @Override
    protected String promptBase() {
        String base = super.promptBase();
        if (!base.contains(ANCLA_CATALOGO)) {
            // Si alguien cambia el texto de la base, las reglas de WhatsApp no se pueden perder en silencio.
            log.warn("ChatbotWhatsappService: no se encontró el ancla del catálogo en promptBase(); "
                    + "las reglas de WhatsApp se agregan al final");
            return base + conTienda(REGLAS_WHATSAPP);
        }
        return base.replace(ANCLA_CATALOGO, conTienda(REGLAS_WHATSAPP) + ANCLA_CATALOGO);
    }

    /**
     * Primera fase: WhatsApp contesta solo con texto. Las fotos y los botones se suman después; hasta
     * entonces el bot no promete mandar imágenes (el cliente se quedaría esperando una que no llega,
     * el mismo problema que tuvo el chat en vivo el 2026-09-17) y manda al cliente a la tienda en línea.
     */
    @Override
    protected String seccionMostrarProductos() {
        return conTienda("""
                CÓMO HABLAR DE PRODUCTOS EN WHATSAPP — SOLO TEXTO POR AHORA:
                - Cuando encuentres el producto, dilo por texto: nombre, presentación y precio. Si hay
                  varios que le sirven, menciónale varios, no solo uno.
                - NUNCA ofrezcas mandar fotos ni imágenes por este chat ni le preguntes si quiere verlas.
                - Si pide fotos, no se las prometas: invítalo a verlas en la tienda en línea
                  {{TIENDA}}/ o ofrécele que una persona del negocio se las mande
                  (en ese caso termina con ##HUMANO##).
                - Ejemplos:
                  * "tienes shorts?" → "¡Sí! Tenemos el Jeans Short Especial (short chico) a $250 MXN y el
                    Surprise SU8183 (short mezclilla) a $280 MXN 😊 ¿Te digo qué tallas hay?"
                  * "me mandas foto?" → "Por aquí no te puedo mandar fotos, pero las puedes ver en la tienda
                    en línea: {{TIENDA}}/ 😊 ¿O prefieres que una persona del negocio
                    te las mande?"

                """);
    }

    /**
     * Contesta un mensaje de WhatsApp. El historial es el hilo ya guardado (incluye lo que contestó una
     * persona), no lo que traiga el cliente: así el bot no se pisa con ella.
     */
    public Mono<String> responder(String mensaje, List<Turno> historial, boolean esPrimeraVez) {
        return responder(mensaje, historial, esPrimeraVez, null, true);
    }

    /**
     * {@code horarioDeAtencion}: texto del horario del negocio («10:00 a.m. a 6:00 p.m.»), o null si el negocio no
     * tiene uno. Es lo único que el bot puede decir cuando preguntan por el horario (regla R12).
     */
    public Mono<String> responder(String mensaje, List<Turno> historial, boolean esPrimeraVez,
                                  String horarioDeAtencion, boolean hayPersonaAhora) {
        return responder(mensaje, historial, esPrimeraVez, horarioDeAtencion, hayPersonaAhora, null);
    }

    /**
     * {@code contextoDePedido}: datos de la conversación para tomar un pedido (número del cliente, lo que ya dio), o
     * null si el bot no toma pedidos (fase 2, regla R16): entonces el modelo no recibe ninguna regla de pedidos.
     */
    public Mono<String> responder(String mensaje, List<Turno> historial, boolean esPrimeraVez,
                                  String horarioDeAtencion, boolean hayPersonaAhora, String contextoDePedido) {
        return responder(mensaje, historial, esPrimeraVez, horarioDeAtencion, hayPersonaAhora, contextoDePedido,
                InformacionDelNegocio.vacia());
    }

    /**
     * {@code negocio}: dirección, mapa y redes que el negocio tiene capturados (regla R22). Sin ellos el bot manda a la
     * tienda en línea a ver la ubicación; con ellos los da tal cual y NUNCA dice que no hay ubicación.
     */
    public Mono<String> responder(String mensaje, List<Turno> historial, boolean esPrimeraVez,
                                  String horarioDeAtencion, boolean hayPersonaAhora, String contextoDePedido,
                                  InformacionDelNegocio negocio) {
        List<Turno> hilo = historial == null ? List.of() : recientes(historial);
        String categoria = detectarCategoriaEnConversacion(mensaje, textosDelCliente(hilo));
        boolean tomaPedidos = contextoDePedido != null && pedidosHabilitados;

        List<Map<String, String>> mensajes = new ArrayList<>();
        String catalogo = obtenerContextoVariantes(categoria);
        if (tomaPedidos) {
            // El catálogo general es solo una página: lo que el cliente nombró se suma para que el artículo SÍ tenga «ref».
            List<String> dicho = new ArrayList<>(textosDelCliente(hilo));
            dicho.add(mensaje);
            catalogo += coincidenciasConLoPedido(dicho, false);
        }
        mensajes.add(Map.of("role", "system", "content", promptBase() + catalogo));
        mensajes.add(Map.of("role", "system", "content", seccionHorario(horarioDeAtencion, hayPersonaAhora)));
        mensajes.add(Map.of("role", "system", "content",
                seccionDatosDelNegocio(negocio, horarioDeAtencion, baseTienda)));
        if (tomaPedidos) {
            mensajes.add(Map.of("role", "system", "content", conTienda(REGLAS_PEDIDO) + contextoDePedido));
        }
        if (esPrimeraVez) {
            mensajes.add(Map.of("role", "system", "content", PRIMER_MENSAJE));
        }
        mensajes.addAll(historialParaModelo(hilo));
        mensajes.add(Map.of("role", "user", "content", mensaje));

        log.info("WhatsApp: prompt armado (categoría={}, {} mensajes al modelo), llamando a la IA",
                categoria, mensajes.size());
        return llamarOpenAIConversacion(mensajes);
    }

    /**
     * Sección «DATOS DEL NEGOCIO» (regla R22): lo que el negocio capturó en Sistema → Negocio y contactos, con sus valores
     * EXACTOS. Un dato que no existe no se menciona. Sin dirección ni coordenadas el bot no afirma ni niega un local: manda
     * a {@code {{TIENDA}}/login}, donde la tienda en línea muestra la ubicación.
     */
    static String seccionDatosDelNegocio(InformacionDelNegocio negocio, String horarioDeAtencion, String baseTienda) {
        InformacionDelNegocio n = negocio == null ? InformacionDelNegocio.vacia() : negocio;
        StringBuilder sb = new StringBuilder("DATOS DEL NEGOCIO (esto es TODO lo que sabes del local y de sus redes; no inventes nada más):\n");
        String direccion = n.direccionLimpia();
        String mapa = n.ligaDelMapa();
        if (direccion != null) {
            sb.append("- Dirección del local: ").append(direccion).append('\n');
        }
        if (mapa != null) {
            sb.append("- Liga del mapa (se abre en Google Maps): ").append(mapa).append('\n');
        }
        if (horarioDeAtencion != null && !horarioDeAtencion.isBlank()) {
            sb.append("- Horario: ").append(horarioDeAtencion.trim()).append('\n');
        }
        agregarRed(sb, "WhatsApp", n.whatsappLimpio());
        agregarRed(sb, "Facebook", n.facebookLimpio());
        agregarRed(sb, "Instagram", n.instagramLimpio());
        agregarRed(sb, "TikTok", n.tiktokLimpio());

        if (n.tieneUbicacion()) {
            sb.append("""
                    REGLAS DE ESTOS DATOS:
                    - El negocio SÍ tiene un local al que se puede ir, aunque también vendemos en línea. Si preguntan dónde
                      está, dónde se ubican, dónde queda, cómo llegar, o dicen que quieren ir a ver o a comprar algo en
                      persona: da la dirección y la liga del mapa que aparecen arriba (solo las que existan) y, si hay horario,
                      dilo. NUNCA digas que no tenemos ubicación ni que solo vendemos en línea.
                    - Eso es la UBICACIÓN: NO listes las zonas de entrega al contestarla.
                    - No agregues indicaciones, referencias ni datos del local que no estén escritos arriba.
                    - Ejemplos:
                      * «Disculpa, ¿dónde te ubicas?» → da la dirección (y el mapa si lo hay) y el horario, sin hablar de entregas.
                      * «¿Entregan en Tejupilco?» → eso es entrega: contesta con las zonas de POLÍTICAS DE LA TIENDA.
                    """);
        } else {
            sb.append("""
                    UBICACIÓN: el negocio no tiene capturada una dirección ni un punto en el mapa. Si preguntan dónde está el
                    local o cómo llegar: NO inventes ninguna dirección, NO listes las zonas de entrega como si fueran la
                    ubicación y NO los mandes a ninguna página a buscarla. Dile con amabilidad que la dirección exacta se la
                    confirma una persona del negocio. Si POLÍTICAS DE LA TIENDA dice que también se puede recoger en el local,
                    sí hay local: dilo, y que la dirección se la confirma una persona.
                    """);
        }
        if (n.whatsappLimpio() != null || n.facebookLimpio() != null || n.instagramLimpio() != null
                || n.tiktokLimpio() != null) {
            sb.append("""
                    REDES: si piden nuestras redes o una en particular, da SOLO los links de arriba, tal cual. Si piden una red
                    que no aparece arriba, dile que por ahora no la tenemos y no inventes ningún link.
                    """);
        } else {
            sb.append("""
                    REDES: no hay ningún link de redes configurado. Si piden las redes, no inventes ni des ningún link: ofrece
                    que una persona del negocio se las pase.
                    """);
        }
        sb.append('\n');
        String base = baseTienda == null ? "" : baseTienda;
        return sb.toString().replace("{{TIENDA}}", base);
    }

    private static void agregarRed(StringBuilder sb, String nombre, String url) {
        if (url != null) {
            sb.append("- ").append(nombre).append(": ").append(url).append('\n');
        }
    }

    static String seccionHorario(String horarioDeAtencion, boolean hayPersonaAhora) {
        if (horarioDeAtencion == null || horarioDeAtencion.isBlank()) {
            return "HORARIO: el negocio no tiene un horario configurado. Si preguntan por el horario, no des ni "
                    + "inventes ninguno: dile con amabilidad que una persona del negocio se lo confirma.";
        }
        String base = "HORARIO DE ATENCIÓN CON UNA PERSONA: de " + horarioDeAtencion + ". Si preguntan por el horario, "
                + "dilo así. Tú contestas a cualquier hora. No des ningún otro horario ni días de atención.";
        if (hayPersonaAhora) {
            return base;
        }
        return base + " AHORA MISMO NO HAY NINGUNA PERSONA DISPONIBLE (estamos fuera de ese horario). Por eso: NUNCA "
                + "digas que \"en un momento lo atienden\". Si el cliente pide una persona, o preguntas algo que no sabes "
                + "(no lo inventes), escribe ##HUMANO## al final: el sistema le avisa el horario y deja la conversación "
                + "marcada para que la atiendan dentro del horario. Pregúntale si quiere que mientras tanto lo sigas "
                + "ayudando tú. Si ya se lo dijiste, no lo repitas: sigue ayudándole.";
    }

    /**
     * Solo se ofrecen artículos con imagen (decisión del dueño, 2026-11-27): igual que la tienda en línea, un artículo
     * sin foto no se muestra. El filtro vive aquí, solo en WhatsApp: los demás bots no cambian.
     */
    @Override
    protected List<Variantes> filtrarCatalogoDelCanal(List<Variantes> variantes) {
        if (variantes == null || variantes.isEmpty() || varianteImagenRepository == null) {
            return variantes;
        }
        List<Integer> ids = variantes.stream().map(Variantes::getId).toList();
        java.util.Set<Integer> conImagen = new java.util.HashSet<>(varianteImagenRepository.idsConImagen(ids));
        return variantes.stream().filter(v -> conImagen.contains(v.getId())).toList();
    }

    /** Solo con la fase 2 encendida: la «ref» deja que el modelo diga cuál artículo sin inventar un id (regla R16). */
    @Override
    protected String referenciaDeArticulo(Variantes v) {
        return pedidosHabilitados ? " [ref " + v.getId() + "]" : "";
    }

    private static List<Turno> recientes(List<Turno> historial) {
        return historial.size() > MAX_HISTORIAL
                ? historial.subList(historial.size() - MAX_HISTORIAL, historial.size())
                : historial;
    }

    private static List<Map<String, String>> historialParaModelo(List<Turno> hilo) {
        List<Map<String, String>> resultado = new ArrayList<>();
        for (Turno t : hilo) {
            if (t.texto() == null || t.texto().isBlank()) {
                continue;
            }
            switch (t.rol()) {
                case CLIENTE -> resultado.add(Map.of("role", "user", "content", t.texto()));
                // Se marca para que el modelo sepa que eso lo contestó una persona y no lo repita ni lo contradiga.
                case PERSONA -> resultado.add(Map.of("role", "assistant", "content", "(persona del negocio) " + t.texto()));
                case BOT -> resultado.add(Map.of("role", "assistant", "content", t.texto()));
            }
        }
        return resultado;
    }

    private static List<String> textosDelCliente(List<Turno> hilo) {
        return hilo.stream()
                .filter(t -> t.rol() == Turno.Rol.CLIENTE)
                .map(Turno::texto)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }
}
