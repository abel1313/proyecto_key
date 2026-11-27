package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Lo que el SISTEMA le dice al cliente sobre su pedido (regla R16). Es texto fijo, no del modelo: el resumen, los
 * precios y la confirmación nunca dependen de lo que la IA escriba. Evita las frases que el filtro de «algo interno»
 * detiene ({@code PoliticaDeRespuesta}): nada de «administrador», «escalar», etc.
 */
public final class TextosDePedido {

    static final String PRIVACIDAD = "https://shop.novedades-jade.com.mx/privacidad";

    private TextosDePedido() {
    }

    public static String faltan(List<Falta> faltas) {
        if (faltas.contains(Falta.TELEFONO)) {
            return "Para cerrar tu pedido necesito un teléfono de contacto (10 dígitos): sin él no lo puedo registrar, "
                    + "porque una persona del negocio necesita llamarte o escribirte para acordar la entrega 💖 "
                    + "¿Me lo compartes? Si quieres usamos este mismo número de WhatsApp.";
        }
        if (faltas.contains(Falta.LINEAS)) {
            return "Para que quepa bien tu pedido, hazlo con máximo " + ValidadorDeSolicitud.MAX_LINEAS
                    + " artículos distintos; el resto lo apartamos en otro pedido 😊 ¿Con cuáles empezamos?";
        }
        String lista = faltas.stream().map(Falta::texto).collect(Collectors.joining(", "));
        return "Para cerrar tu pedido me falta: " + lista + " 😊";
    }

    public static String articuloNoEncontrado() {
        return "No encontré ese artículo para apartarlo 😕 ¿Me dices cuál es? Puedes mandarme el nombre tal como lo viste.";
    }

    /** Nunca se dice cuántas piezas hay (regla de todos los canales: así no se sondea el inventario). */
    public static String noDisponible(String nombre, boolean hayAlgo) {
        if (!hayAlgo) {
            return "Ya no tengo disponible *" + nombre + "* 😕 ¿Te muestro algo parecido?";
        }
        return "No tengo esa cantidad de *" + nombre + "* 😕 ¿Te aparto menos piezas o prefieres otra cosa?";
    }

    public static String lugarNoValido(String lugar, List<String> zonas, boolean hayLocal) {
        String donde = zonas.isEmpty() ? "" : " Llevamos a: " + String.join(", ", zonas) + ".";
        return "Por ahora no llevamos pedidos a " + lugar + "." + donde
                + (hayLocal ? " También puedes recogerlo en el local 💖 ¿Qué prefieres?" : " ¿Quieres escoger otro lugar?");
    }

    public static String sinTelefonoPasaAPersona() {
        return "No logro registrar tu teléfono por aquí 😕 Una persona del negocio te va a ayudar con tu pedido 💖";
    }

    public static String marcaRota() {
        return "Perdón, no alcancé a registrar tu pedido 🙈 ¿Me confirmas otra vez los datos (artículo, nombre, "
                + "teléfono, correo, quién recibe y dónde)?";
    }

    public static String pidioCancelar(int numero) {
        return "Anoté que quieres cancelar el pedido #" + numero + ". Una persona del negocio lo revisa y te avisa 💖";
    }

    public static String cancelarSinNumero() {
        return "Para cancelar necesito el número de tu pedido: te llegó en el correo del ticket (dice «Pedido #…»). "
                + "¿Me lo compartes? 😊";
    }

    /** {@code horarioSiCerrado}: texto del horario si ahorita no hay nadie (null = hay una persona disponible). */
    public static String falloAlCrear(String horarioSiCerrado) {
        String cuando = horarioSiCerrado == null ? "te va a contactar muy pronto"
                : "te va a contactar en nuestro horario de atención, de " + horarioSiCerrado;
        return "Tuve un problema para registrar tu pedido 😕 No se hizo ningún cargo ni apartado. Una persona del "
                + "negocio " + cuando + " para ayudarte 💖";
    }

    public static final String PREGUNTA_CONFIRMAR = "¿Confirmas tu pedido?";

    public static String soloSi() {
        return "Para confirmar tu pedido responde solo *SÍ* 😊 Si quieres cambiar algo, dime qué.";
    }

    public static String resumenVencio() {
        return "Tu resumen ya venció, así que todavía no hice el pedido 🙈 ¿Me confirmas otra vez los datos (artículo, "
                + "cantidad y a dónde lo llevamos) para armártelo de nuevo?";
    }

    public static String precioCambio() {
        return "Mientras decidías cambió el precio de algo del pedido, así que te lo armo de nuevo con los precios de hoy 😊\n\n";
    }

    public static String topeDeApartados(int abiertos) {
        return "Ya tienes " + abiertos + (abiertos == 1 ? " pedido apartado esperando" : " pedidos apartados esperando") + ". Antes de hacer otro, una persona del negocio se "
                + "pondrá en contacto contigo para ayudarte 💖";
    }

    public static String resumen(BorradorDePedido b) {
        StringBuilder sb = new StringBuilder("Este es el resumen de tu pedido 💖\n");
        for (LineaDePedido l : b.lineas()) {
            sb.append("• ").append(l.cantidad()).append(" × ").append(corto(l.nombre(), 80)).append(" — ")
                    .append(dinero(l.precioUnitario())).append(" c/u = ").append(dinero(l.subtotal())).append('\n');
        }
        sb.append("*Total: ").append(dinero(b.total())).append("*\n");
        if (b.recogerEnLocal()) {
            sb.append("Entrega: lo recoges en el local (recibe: ").append(b.recibe()).append(")\n");
        } else {
            sb.append("Entrega: ").append(b.lugar().nombre()).append(", ").append(b.direccion())
                    .append(" (recibe: ").append(b.recibe()).append(")\n");
        }
        sb.append("Contacto: ").append(b.nombre()).append(" · ").append(b.telefono()).append(" · ").append(b.correo()).append('\n');
        if (b.observacionesCliente() != null && !b.observacionesCliente().isBlank()) {
            sb.append("Nota: ").append(b.observacionesCliente().trim()).append('\n');
        }
        sb.append("\nSe queda *apartado*: no se paga nada ahora. Una persona del negocio se pondrá en contacto contigo "
                + "para acordar ");
        sb.append(b.recogerEnLocal() ? "cuándo pasas por él" : "el día y la hora de entrega");
        sb.append(" y el pago (solo efectivo o transferencia).");
        if (b.irPagando()) {
            sb.append(" Anoté que quieres ir pagando: una persona te confirma el enganche.");
        }
        sb.append("\n\n" + PREGUNTA_CONFIRMAR + " Responde *SÍ* para hacerlo, o dime qué quieres cambiar. "
                + "Datos personales: ").append(PRIVACIDAD);
        return sb.toString();
    }

    /**
     * El mensaje final con lo que el sistema de verdad cobró (si hubo precio de grupo o de campaña el total es menor
     * que el del resumen y se dice).
     */
    public static String creado(int pedidoId, double total, BorradorDePedido b, boolean ticketSalio,
                                java.util.List<ResultadoDeCreacion.LineaCobrada> lineas) {
        StringBuilder sb = new StringBuilder("¡Listo! Tu pedido *#").append(pedidoId).append("* quedó apartado 🎉\n");
        if (!lineas.isEmpty()) {
            for (ResultadoDeCreacion.LineaCobrada l : lineas) {
                sb.append("• ").append(l.cantidad()).append(" × ").append(corto(l.nombre(), 80)).append(" — ")
                        .append(dinero(l.precioCobrado())).append(" c/u\n");
            }
        }
        sb.append("*Total: ").append(dinero(total)).append("*");
        if (Math.abs(total - b.total()) > 0.005) {
            sb.append(" (te quedó mejor que en el resumen por una promoción)");
        }
        sb.append('\n');
        sb.append(ticketSalio
                ? "Te mandamos el ticket a " + b.correo() + " (revisa también tu bandeja de spam).\n"
                : "No pude mandarte el ticket por correo, pero una persona del negocio te lo hará llegar.\n");
        sb.append(b.recogerEnLocal()
                ? "Una persona se pondrá en contacto contigo para acordar cuándo pasas a recogerlo y el pago."
                : "Una persona se pondrá en contacto contigo para acordar el día, la hora de entrega y el pago.");
        sb.append(" ¡Gracias por tu compra! 💖");
        return sb.toString();
    }

    static String corto(String t, int max) {
        if (t == null) {
            return "";
        }
        return t.length() > max ? t.substring(0, max - 1) + "…" : t;
    }

    public static String yaNoSePudo() {
        return "Justo ahora ese artículo ya no está disponible en esa cantidad 😕 No se hizo el pedido. ¿Quieres cambiar "
                + "la cantidad o escoger otro?";
    }

    static String dinero(double d) {
        return "$" + String.format(Locale.US, "%,.2f", d).replace(".00", "");
    }

    /** Para comparar el lugar que dijo el cliente con los del catálogo: minúsculas, sin acentos ni signos. */
    public static String clave(String t) {
        if (t == null) {
            return "";
        }
        String sin = Normalizer.normalize(t.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sin.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
