package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReporteCalculado;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.ColumnasReporte.Columna;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * [Hexagonal: Driven Adapter] El reporte en PDF con Apache PDFBox. Hoja A3 horizontal:
 * banda con el nombre del negocio, sus datos de contacto, quién y cuándo lo generó, el periodo y los filtros, tarjetas con los totales,
 * la tabla (encabezado que se repite en cada hoja, filas alternadas, rojo si hay pérdida, «SIN FLETE» en ámbar), la fila de totales y un pie con
 * el número de página. El texto que no cabe en su columna se corta con «...».
 */
public class PdfReporteGenerador {

    // Verde de la marca (el mismo de los correos del negocio) y neutros.
    private static final Color VERDE = new Color(0x00, 0x87, 0x5A);
    private static final Color VERDE_SUAVE = new Color(0xE6, 0xF4, 0xEE);
    private static final Color GRIS_FILA = new Color(0xF6, 0xF7, 0xF9);
    private static final Color LINEA = new Color(0xD5, 0xD9, 0xE0);
    private static final Color TEXTO = new Color(0x1F, 0x29, 0x37);
    private static final Color TEXTO_SUAVE = new Color(0x6B, 0x72, 0x80);
    private static final Color ROJO = new Color(0xB9, 0x1C, 0x1C);
    private static final Color AMBAR = new Color(0xB4, 0x53, 0x09);
    private static final Color BLANCO = Color.WHITE;

    private static final float MARGEN = 28f;
    private static final float ALTO_FILA = 15.5f;
    private static final float TAM = 6.4f;
    private static final float PAD = 3f;
    private static final float ALTO_BANDA = 74f;

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);

    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generar(ReporteCalculado r, EncabezadoReporte enc) {
        PDRectangle hoja = new PDRectangle(PDRectangle.A3.getHeight(), PDRectangle.A3.getWidth());   // horizontal
        List<Columna> cols = ColumnasReporte.COLUMNAS;
        float[] x = posiciones(cols.size(), hoja.getWidth() - 2 * MARGEN);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(hoja);
            doc.addPage(pagina);
            PDPageContentStream cs = new PDPageContentStream(doc, pagina);
            float y = portada(cs, hoja, r, enc);
            y = fila(cs, cols, x, y, Columna::titulo, null, negrita, VERDE, BLANCO, true, false);

            int n = 0;
            for (FilaReporte f : r.filas()) {
                if (y - ALTO_FILA < MARGEN + ALTO_FILA + 14) {       // no cabe otra fila más la de totales y el pie: hoja nueva
                    cs.close();
                    pagina = new PDPage(hoja);
                    doc.addPage(pagina);
                    cs = new PDPageContentStream(doc, pagina);
                    y = cabeceraCorta(cs, hoja, enc);
                    y = fila(cs, cols, x, y, Columna::titulo, null, negrita, VERDE, BLANCO, true, false);
                }
                final boolean sinVentas = f.base().piezas() == 0;
                y = fila(cs, cols, x, y, c -> texto(c, c.valor().apply(f)), c -> colorCelda(c, f, sinVentas),
                        normal, (n++ % 2 == 1) ? GRIS_FILA : null, TEXTO, false, false);
            }
            if (y - ALTO_FILA < MARGEN + 14) {
                cs.close();
                pagina = new PDPage(hoja);
                doc.addPage(pagina);
                cs = new PDPageContentStream(doc, pagina);
                y = cabeceraCorta(cs, hoja, enc);
                y = fila(cs, cols, x, y, Columna::titulo, null, negrita, VERDE, BLANCO, true, false);
            }
            fila(cs, cols, x, y, c -> texto(c, c.total().apply(r.totales())), null, negrita, VERDE_SUAVE, TEXTO, false, true);
            cs.close();

            // Pie con el número de página (se pone al final, cuando ya se sabe cuántas hojas son).
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream pie = new PDPageContentStream(doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    float py = MARGEN - 8;
                    linea(pie, MARGEN, py + 11, hoja.getWidth() - MARGEN, py + 11, LINEA, 0.5f);
                    texto(pie, normal, 7, MARGEN, py, TEXTO_SUAVE, enc.negocio() + " · " + EncabezadoReporte.TITULO + " · Uso interno: incluye costos y ganancias");
                    derecha(pie, normal, 7, hoja.getWidth() - MARGEN, py, TEXTO_SUAVE, "Página " + (i + 1) + " de " + total);
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el PDF del reporte.", e);
        }
    }

    // ── Primera hoja: banda, datos, filtros y tarjetas ──

    private float portada(PDPageContentStream cs, PDRectangle hoja, ReporteCalculado r, EncabezadoReporte enc) throws IOException {
        float ancho = hoja.getWidth();
        float top = hoja.getHeight();
        rect(cs, 0, top - ALTO_BANDA, ancho, ALTO_BANDA, VERDE);
        texto(cs, negrita, 24, MARGEN, top - 36, BLANCO, enc.negocio().toUpperCase(Locale.ROOT));
        texto(cs, normal, 12, MARGEN, top - 56, BLANCO, EncabezadoReporte.TITULO);
        derecha(cs, negrita, 9, ancho - MARGEN, top - 30, BLANCO, "Generado el " + FECHA_HORA.format(enc.generado()));
        if (enc.generadoPor() != null && !enc.generadoPor().isBlank()) derecha(cs, normal, 9, ancho - MARGEN, top - 44, BLANCO, "Por: " + enc.generadoPor());

        float y = top - ALTO_BANDA - 14;
        if (!enc.lineaContacto().isEmpty()) {
            texto(cs, normal, 8, MARGEN, y, TEXTO_SUAVE, enc.lineaContacto());
            y -= 14;
        }
        texto(cs, negrita, 9.5f, MARGEN, y, TEXTO, ColumnasReporte.descripcion(r.filtro()));
        y -= 10;

        // Tarjetas con los totales (de TODAS las filas).
        TotalesReporte t = r.totales();
        float gap = 8f;
        float alto = 40f;
        int n = 6;
        float w = (ancho - 2 * MARGEN - gap * (n - 1)) / n;
        float top0 = y - 4;
        String[][] tarjetas = {
                {"PRODUCTOS", String.valueOf(t.productos()), t.productosVendidos() + " vendidos · " + t.productosSinVender() + " sin vender", null},
                {"PIEZAS VENDIDAS", fmt(t.piezas()), fmt(t.piezasPorVender()) + " en stock por vender", null},
                {"TOTAL VENDIDO", dinero(t.total()), "Costo " + dinero(t.costo()), null},
                {"GANANCIA VENDIDA", dinero(t.gananciaVendida()), t.margenRealPct() == null ? "" : pct(t.margenRealPct()) + " real", "v"},
                {"GANANCIA POR VENDER", dinero(t.gananciaPorVender()), "Si se vende todo el stock", null},
                {"GANANCIA TOTAL", dinero(t.gananciaTotal()), "Vendida + por vender", "v"}};
        for (int i = 0; i < n; i++) {
            float cx = MARGEN + i * (w + gap);
            rect(cs, cx, top0 - alto, w, alto, VERDE_SUAVE);
            rect(cs, cx, top0 - alto, 3, alto, VERDE);
            texto(cs, negrita, 6.5f, cx + 9, top0 - 11, TEXTO_SUAVE, tarjetas[i][0]);
            Color colorValor = tarjetas[i][3] == null ? TEXTO : (esNegativo(tarjetas[i][1]) ? ROJO : VERDE);
            texto(cs, negrita, 14, cx + 9, top0 - 26, colorValor, tarjetas[i][1]);
            texto(cs, normal, 6.5f, cx + 9, top0 - 36, TEXTO_SUAVE, tarjetas[i][2]);
        }
        return top0 - alto - 14;
    }

    /** Las hojas siguientes llevan solo una franja delgada con el nombre del negocio. */
    private float cabeceraCorta(PDPageContentStream cs, PDRectangle hoja, EncabezadoReporte enc) throws IOException {
        float top = hoja.getHeight();
        rect(cs, 0, top - 24, hoja.getWidth(), 24, VERDE);
        texto(cs, negrita, 10, MARGEN, top - 16, BLANCO, enc.negocio().toUpperCase(Locale.ROOT) + "  ·  " + EncabezadoReporte.TITULO);
        return top - 24 - 12;
    }

    // ── Tabla ──

    @FunctionalInterface
    private interface Contenido { String de(Columna c); }

    @FunctionalInterface
    private interface ColorDe { Color de(Columna c); }

    /** Dibuja una fila (títulos, datos o totales) y devuelve la «y» donde queda su borde de abajo. */
    private float fila(PDPageContentStream cs, List<Columna> cols, float[] x, float y, Contenido contenido, ColorDe colorDe, PDFont fuente,
                       Color fondo, Color colorTexto, boolean titulos, boolean totales) throws IOException {
        float abajo = y - ALTO_FILA;
        if (fondo != null) rect(cs, x[0], abajo, x[cols.size()] - x[0], ALTO_FILA, fondo);
        if (totales) linea(cs, x[0], y, x[cols.size()], y, VERDE, 1.2f);
        else linea(cs, x[0], abajo, x[cols.size()], abajo, LINEA, 0.4f);
        for (int i = 0; i < cols.size(); i++) {
            Columna c = cols.get(i);
            String t = contenido.de(c);
            if (t == null || t.isEmpty()) continue;
            t = ajustar(fuente, limpiar(fuente, t), x[i + 1] - x[i] - 2 * PAD);
            float w = fuente.getStringWidth(t) / 1000f * TAM;
            float px;
            if (titulos) px = x[i] + (x[i + 1] - x[i] - w) / 2;
            else if (c.tipo() == ColumnasReporte.Tipo.TEXTO) px = x[i] + PAD;
            else px = x[i + 1] - PAD - w;
            Color color = colorDe != null && colorDe.de(c) != null ? colorDe.de(c) : colorTexto;
            cs.setNonStrokingColor(color);
            cs.beginText();
            cs.setFont(fuente, TAM);
            cs.newLineAtOffset(px, abajo + 4.8f);
            cs.showText(t);
            cs.endText();
        }
        return abajo;
    }

    /** Rojo si hay pérdida, ámbar en «SIN FLETE», gris si el producto no se vendió. */
    private static Color colorCelda(Columna c, FilaReporte f, boolean sinVentas) {
        Object v = c.valor().apply(f);
        if (v instanceof BigDecimal b && c.tipo() == ColumnasReporte.Tipo.DINERO && b.signum() < 0) return ROJO;
        if (c.titulo().equals("¿Flete?") && f.sinFlete()) return AMBAR;
        return sinVentas ? TEXTO_SUAVE : null;
    }

    // ── Dibujo y texto ──

    private static void rect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void linea(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color c, float grosor) throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(grosor);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void texto(PDPageContentStream cs, PDFont fuente, float tam, float px, float py, Color color, String t) throws IOException {
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(fuente, tam);
        cs.newLineAtOffset(px, py);
        cs.showText(limpiar(fuente, t));
        cs.endText();
    }

    private void derecha(PDPageContentStream cs, PDFont fuente, float tam, float derechaX, float py, Color color, String t) throws IOException {
        String limpio = limpiar(fuente, t);
        texto(cs, fuente, tam, derechaX - fuente.getStringWidth(limpio) / 1000f * tam, py, color, limpio);
    }

    /** Los títulos y la tabla se reparten el ancho con estos pesos (el nombre y el código de barras necesitan más). */
    private static float[] posiciones(int n, float ancho) {
        float[] pesos = new float[n];
        float suma = 0;
        for (int i = 0; i < n; i++) {
            pesos[i] = i == 0 ? 3.0f : i == 1 ? 2.0f : i == 2 ? 1.5f : (i == 11 || i == 16 || i == 17) ? 2.3f : (i == 6 || i == 7) ? 2.1f : 1.8f;
            suma += pesos[i];
        }
        float[] x = new float[n + 1];
        x[0] = MARGEN;
        for (int i = 0; i < n; i++) x[i + 1] = x[i] + ancho * pesos[i] / suma;
        return x;
    }

    /** Quita lo que la fuente estándar no sabe dibujar (emojis, etc.) para que un nombre raro no tire todo el PDF. */
    private static String limpiar(PDFont fuente, String t) {
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            try {
                fuente.encode(String.valueOf(ch));
                sb.append(ch);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static String ajustar(PDFont fuente, String t, float ancho) throws IOException {
        if (fuente.getStringWidth(t) / 1000f * TAM <= ancho) return t;
        String corte = t;
        while (corte.length() > 1 && fuente.getStringWidth(corte + "...") / 1000f * TAM > ancho) corte = corte.substring(0, corte.length() - 1);
        return corte + "...";
    }

    private static String dinero(BigDecimal b) { return "$" + new DecimalFormat("#,##0.00", US).format(b); }
    private static String pct(BigDecimal b) { return new DecimalFormat("0.00", US).format(b) + "%"; }
    private static String fmt(long n) { return new DecimalFormat("#,##0", US).format(n); }
    private static boolean esNegativo(String dinero) { return dinero.startsWith("$-") || dinero.startsWith("-"); }

    private static String texto(Columna c, Object v) {
        if (v == null) return "";
        if (!ColumnasReporte.esNumero(v)) return String.valueOf(v);
        BigDecimal b = v instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) v).doubleValue());
        return switch (c.tipo()) {
            case DINERO -> dinero(b);
            case PORCENTAJE -> pct(b);
            default -> new DecimalFormat("#,##0", US).format(b);
        };
    }
}
