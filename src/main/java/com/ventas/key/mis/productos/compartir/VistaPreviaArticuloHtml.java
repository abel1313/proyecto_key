package com.ventas.key.mis.productos.compartir;

/**
 * HTML mínimo para las vistas previas de WhatsApp, Facebook y Google (Open Graph + marcado Product).
 * Los rastreadores de esas apps no ejecutan JavaScript, así que sin esto un enlace de la tienda (una SPA)
 * sale sin foto ni nombre. Java puro: no depende de Spring.
 */
public final class VistaPreviaArticuloHtml {

    private VistaPreviaArticuloHtml() { }

    /** @param imagenUrl absoluta o vacía; @param urlTienda a dónde va una persona real (puede ir vacía) */
    public static String armar(String titulo, String descripcion, String precio, String imagenUrl, String urlTienda, String urlCanonica) {
        String t = esc(titulo), d = esc(recortar(descripcion, 200)), p = esc(precio), i = esc(imagenUrl), u = esc(urlTienda), c = esc(urlCanonica);
        StringBuilder h = new StringBuilder(1200);
        h.append("<!doctype html><html lang=\"es-MX\"><head><meta charset=\"utf-8\">")
         .append("<title>").append(t).append(" · Novedades Jade</title>")
         .append("<meta name=\"description\" content=\"").append(d).append("\">")
         .append("<meta property=\"og:type\" content=\"product\">")
         .append("<meta property=\"og:site_name\" content=\"Novedades Jade\">")
         .append("<meta property=\"og:title\" content=\"").append(t).append("\">")
         .append("<meta property=\"og:description\" content=\"").append(d).append("\">");
        if (!i.isEmpty()) h.append("<meta property=\"og:image\" content=\"").append(i).append("\">");
        if (!c.isEmpty()) h.append("<meta property=\"og:url\" content=\"").append(c).append("\">");
        if (!p.isEmpty()) h.append("<meta property=\"product:price:amount\" content=\"").append(p).append("\">")
         .append("<meta property=\"product:price:currency\" content=\"MXN\">");
        h.append("<meta name=\"twitter:card\" content=\"summary_large_image\">");
        if (!u.isEmpty()) h.append("<link rel=\"canonical\" href=\"").append(u).append("\">");
        if (!u.isEmpty()) h.append("<meta http-equiv=\"refresh\" content=\"0;url=").append(u).append("\">");
        h.append("</head><body><h1>").append(t).append("</h1><p>").append(d).append("</p>");
        if (!p.isEmpty()) h.append("<p>$").append(p).append(" MXN</p>");
        if (!u.isEmpty()) h.append("<p><a href=\"").append(u).append("\">Ver en la tienda</a></p>");
        h.append("</body></html>");
        return h.toString();
    }

    /** Escapa lo que entra a un atributo o texto HTML: el nombre y la descripción los escribe una persona. */
    static String esc(String s) {
        if (s == null) return "";
        StringBuilder o = new StringBuilder(s.length() + 16);
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> o.append("&amp;");
                case '<' -> o.append("&lt;");
                case '>' -> o.append("&gt;");
                case '"' -> o.append("&quot;");
                case '\'' -> o.append("&#39;");
                case '\n', '\r' -> o.append(' ');
                default -> o.append(ch);
            }
        }
        return o.toString();
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        String x = s.strip();
        if (x.codePointCount(0, x.length()) <= max) return x;
        return x.substring(0, x.offsetByCodePoints(0, max - 1)) + "…";   // no parte un emoji (par sustituto) a la mitad
    }
}
