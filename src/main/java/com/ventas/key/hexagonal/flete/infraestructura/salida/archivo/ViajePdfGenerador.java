package com.ventas.key.hexagonal.flete.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.flete.dominio.modelo.ReporteDeViaje;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] El reporte de un viaje en PDF (Apache PDFBox 3.0.3), hoja carta vertical: banda con el nombre del negocio, datos del viaje,
 * cómo va el flete, productos y ventas ligadas (la tabla de ventas sigue en otra hoja si no cabe). El texto que no cabe en su columna se corta con «...».
 */
@Component
public class ViajePdfGenerador {

    private static final Color VERDE = new Color(0x00, 0x87, 0x5A);
    private static final Color VERDE_SUAVE = new Color(0xE6, 0xF4, 0xEE);
    private static final Color GRIS_FILA = new Color(0xF6, 0xF7, 0xF9);
    private static final Color LINEA = new Color(0xD5, 0xD9, 0xE0);
    private static final Color TEXTO = new Color(0x1F, 0x29, 0x37);
    private static final Color SUAVE = new Color(0x6B, 0x72, 0x80);
    private static final float M = 36f;
    private static final float ALTO_FILA = 16f;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);

    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** El lienzo: la página de turno y la «y» donde se sigue escribiendo (baja de arriba hacia abajo). */
    private final class Hoja implements AutoCloseable {
        final PDDocument doc;
        PDPage pagina;
        PDPageContentStream cs;
        float y;
        int numero;
        final String negocio;

        Hoja(PDDocument doc, String negocio) throws IOException {
            this.doc = doc;
            this.negocio = negocio;
            nueva();
        }

        void nueva() throws IOException {
            if (cs != null) {
                pie();
                cs.close();
            }
            pagina = new PDPage(PDRectangle.LETTER);
            doc.addPage(pagina);
            cs = new PDPageContentStream(doc, pagina);
            numero++;
            y = pagina.getMediaBox().getHeight() - M;
        }

        float ancho() { return pagina.getMediaBox().getWidth() - 2 * M; }

        void asegurar(float alto) throws IOException {
            if (y - alto < M + 20) nueva();
        }

        void pie() throws IOException {
            texto(cs, normal, 7, M, 20, SUAVE, negocio + " · Reporte de viaje de compra · uso interno");
            String p = "Página " + numero;
            texto(cs, normal, 7, pagina.getMediaBox().getWidth() - M - normal.getStringWidth(p) / 1000f * 7, 20, SUAVE, p);
        }

        @Override
        public void close() throws IOException {
            pie();
            cs.close();
        }
    }

    public byte[] generar(ReporteDeViaje r, String negocio, String generadoPor) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (Hoja h = new Hoja(doc, negocio)) {
                var v = r.viaje();
                // Banda
                float w = h.ancho();
                rect(h.cs, M, h.y - 46, w, 46, VERDE);
                texto(h.cs, negrita, 17, M + 10, h.y - 22, Color.WHITE, negocio.toUpperCase());
                texto(h.cs, normal, 9, M + 10, h.y - 38, Color.WHITE, "Reporte de viaje de compra");
                String gen = "Generado por " + generadoPor + " el " + java.time.LocalDateTime.now().format(DIA_HORA);
                texto(h.cs, normal, 8, M + w - 10 - normal.getStringWidth(limpiar(gen)) / 1000f * 8, h.y - 22, Color.WHITE, gen);
                h.y -= 66;

                texto(h.cs, negrita, 14, M, h.y, TEXTO, v.nombre());
                h.y -= 15;
                String linea2 = v.fecha().format(DIA) + (v.nota() != null ? " · " + v.nota() : "") + " · " + ViajeExcelGenerador.etiqueta(r.estado());
                texto(h.cs, normal, 9, M, h.y, SUAVE, linea2);
                h.y -= 20;

                // Tarjetas (4 por renglón, 2 renglones)
                String[][] tarjetas = {
                        {"FLETE TOTAL", dinero(v.fleteTotal()), "gastos que cuentan: " + dinero(v.costoACubrir().subtract(v.fleteTotal()))},
                        {"COSTO A CUBRIR", dinero(v.costoACubrir()), "flete + gastos que cuentan"},
                        {"FLETE RECUPERADO", dinero(r.fleteRecuperado()), r.pagadoEn() == null ? "con lo vendido" : "cubierto el " + r.pagadoEn().format(DIA)},
                        {"FALTA POR CUBRIR", dinero(r.faltaPorCubrir()), r.fleteComoGanancia().signum() > 0 ? "ya es ganancia: " + dinero(r.fleteComoGanancia()) : "lo vendido aún no lo cubre"},
                        {"PIEZAS", r.piezasCompradas() + " compradas", r.piezasVendidas() + " vendidas · " + r.piezasRestantes() + " quedan"},
                        {"TOTAL VENDIDO", dinero(r.totalVendido()), "costo " + dinero(r.costoVendido())},
                        {"GANANCIA VENDIDA", dinero(r.gananciaVendida()), "ya sin comisión de tarjeta"},
                        {"FLETE YA GANANCIA", dinero(r.fleteComoGanancia()), "flete de lo vendido tras cubrirlo"}};
                float gap = 8, cw = (w - 3 * gap) / 4, ch = 44;
                for (int i = 0; i < tarjetas.length; i++) {
                    float cx = M + (i % 4) * (cw + gap);
                    float cy = h.y - (i / 4) * (ch + gap);
                    rect(h.cs, cx, cy - ch, cw, ch, VERDE_SUAVE);
                    rect(h.cs, cx, cy - ch, 3, ch, VERDE);
                    texto(h.cs, negrita, 6.5f, cx + 8, cy - 11, SUAVE, tarjetas[i][0]);
                    texto(h.cs, negrita, 12, cx + 8, cy - 26, TEXTO, corta(negrita, 12, tarjetas[i][1], cw - 14));
                    texto(h.cs, normal, 6.5f, cx + 8, cy - 37, SUAVE, corta(normal, 6.5f, tarjetas[i][2], cw - 14));
                }
                h.y -= 2 * ch + gap + 18;

                // Gastos
                if (!v.gastos().isEmpty()) {
                    texto(h.cs, negrita, 10, M, h.y, TEXTO, "Otros gastos del viaje");
                    h.y -= 13;
                    for (var g : v.gastos()) {
                        h.asegurar(ALTO_FILA);
                        texto(h.cs, normal, 9, M + 4, h.y, TEXTO, corta(normal, 9, g.concepto() + (g.cuentaParaCubrir() ? "" : " (no cuenta para cubrir)"), w - 100));
                        String m = dinero(g.monto());
                        texto(h.cs, normal, 9, M + w - 4 - normal.getStringWidth(m) / 1000f * 9, h.y, TEXTO, m);
                        h.y -= 13;
                    }
                    h.y -= 8;
                }

                // Productos
                texto(h.cs, negrita, 10, M, h.y, TEXTO, "Productos del viaje");
                h.y -= 8;
                float[] pesos = {4.2f, 2.4f, 1.3f, 1.3f, 1.2f, 2f};
                tabla(h, pesos, new String[]{"Producto", "Código de barras", "Compradas", "Vendidas", "Quedan", "Total vendido"}, false);
                int fila = 0;
                for (var p : r.renglones()) {
                    fila(h, pesos, new String[]{p.nombre() == null ? "Producto " + p.productoId() : p.nombre(), p.codigoBarras() == null ? "" : p.codigoBarras(),
                            String.valueOf(p.piezasCompradas()), String.valueOf(p.piezasVendidas()), String.valueOf(p.piezasRestantes()), dinero(p.totalVendido())},
                            new boolean[]{false, false, true, true, true, true}, fila++ % 2 == 1, false);
                }
                h.y -= 16;

                // Ventas ligadas
                h.asegurar(60);
                texto(h.cs, negrita, 10, M, h.y, TEXTO, "Ventas ligadas a este viaje (en el orden en que se recuperó el flete)");
                h.y -= 8;
                float[] pv = {1.1f, 1.5f, 3.6f, 1f, 1.6f, 1.8f, 1.8f, 1.9f};
                String[] tv = {"Venta", "Fecha", "Producto", "Piezas", "Flete x pieza", "Flete recup.", "Flete ganancia", "Total vendido"};
                tabla(h, pv, tv, false);
                Map<Integer, String> nombres = new HashMap<>();
                r.renglones().forEach(p -> nombres.put(p.productoId(), p.nombre()));
                BigDecimal rec = BigDecimal.ZERO, gan = BigDecimal.ZERO, tot = BigDecimal.ZERO;
                int piezas = 0;
                fila = 0;
                if (r.lineas().isEmpty()) {
                    texto(h.cs, normal, 9, M + 4, h.y - 12, SUAVE, "Todavía no hay ventas ligadas a este viaje.");
                    h.y -= 22;
                }
                for (var l : r.lineas()) {
                    var vl = l.venta();
                    if (h.y - ALTO_FILA < M + 20) {
                        h.nueva();
                        tabla(h, pv, tv, true);
                    }
                    fila(h, pv, new String[]{"#" + vl.ventaId(), vl.fechaVenta() == null ? "" : vl.fechaVenta().format(DIA),
                            nombres.getOrDefault(vl.productoId(), "Producto " + vl.productoId()), String.valueOf(vl.cantidad()), dinero(vl.fletePorPieza()),
                            dinero(l.fleteRecuperado()), dinero(l.fleteComoGanancia()), dinero(vl.totalVendido())},
                            new boolean[]{false, false, false, true, true, true, true, true}, fila++ % 2 == 1, false);
                    piezas += vl.cantidad();
                    rec = rec.add(l.fleteRecuperado());
                    gan = gan.add(l.fleteComoGanancia());
                    tot = tot.add(vl.totalVendido());
                }
                if (!r.lineas().isEmpty()) {
                    if (h.y - ALTO_FILA < M + 20) {
                        h.nueva();
                    }
                    fila(h, pv, new String[]{"TOTAL", "", "", String.valueOf(piezas), "", dinero(rec), dinero(gan), dinero(tot)},
                            new boolean[]{false, false, false, true, true, true, true, true}, false, true);
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** El encabezado de una tabla (barra verde con títulos). */
    private void tabla(Hoja h, float[] pesos, String[] titulos, boolean continuacion) throws IOException {
        h.asegurar(ALTO_FILA * 2);
        float[] x = columnas(h, pesos);
        rect(h.cs, M, h.y - ALTO_FILA, h.ancho(), ALTO_FILA, VERDE);
        for (int i = 0; i < titulos.length; i++) {
            float ancho = x[i + 1] - x[i] - 6;
            String t = corta(negrita, 7.5f, titulos[i], ancho);
            float tx = i >= 2 && esNumerica(titulos[i]) ? x[i + 1] - 3 - negrita.getStringWidth(limpiar(t)) / 1000f * 7.5f : x[i] + 3;
            texto(h.cs, negrita, 7.5f, tx, h.y - 11, Color.WHITE, t);
        }
        h.y -= ALTO_FILA;
    }

    private static boolean esNumerica(String titulo) {
        return !titulo.equals("Producto") && !titulo.equals("Código de barras") && !titulo.equals("Fecha") && !titulo.equals("Venta");
    }

    private void fila(Hoja h, float[] pesos, String[] celdas, boolean[] derecha, boolean gris, boolean total) throws IOException {
        if (h.y - ALTO_FILA < M + 20) {
            h.nueva();
        }
        float[] x = columnas(h, pesos);
        if (total) {
            rect(h.cs, M, h.y - ALTO_FILA, h.ancho(), ALTO_FILA, VERDE_SUAVE);
        } else if (gris) {
            rect(h.cs, M, h.y - ALTO_FILA, h.ancho(), ALTO_FILA, GRIS_FILA);
        }
        linea(h.cs, M, h.y - ALTO_FILA, M + h.ancho());
        PDFont f = total ? negrita : normal;
        for (int i = 0; i < celdas.length; i++) {
            String t = corta(f, 8, celdas[i], x[i + 1] - x[i] - 6);
            float tx = derecha[i] ? x[i + 1] - 3 - f.getStringWidth(limpiar(t)) / 1000f * 8 : x[i] + 3;
            texto(h.cs, f, 8, tx, h.y - 11, TEXTO, t);
        }
        h.y -= ALTO_FILA;
    }

    private static float[] columnas(Hoja h, float[] pesos) {
        float suma = 0;
        for (float p : pesos) suma += p;
        float[] x = new float[pesos.length + 1];
        x[0] = M;
        for (int i = 0; i < pesos.length; i++) x[i + 1] = x[i] + h.ancho() * pesos[i] / suma;
        return x;
    }

    private String corta(PDFont f, float tam, String t, float ancho) throws IOException {
        String s = limpiar(t == null ? "" : t);
        if (f.getStringWidth(s) / 1000f * tam <= ancho) return s;
        while (s.length() > 1 && f.getStringWidth(s + "...") / 1000f * tam > ancho) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    /** Quita lo que la fuente estándar no sabe dibujar (emojis, etc.) para que un nombre raro no tire todo el PDF. */
    private String limpiar(String t) {
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            try {
                normal.encode(String.valueOf(ch));
                sb.append(ch);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private void texto(PDPageContentStream cs, PDFont f, float tam, float px, float py, Color c, String t) throws IOException {
        cs.setNonStrokingColor(c);
        cs.beginText();
        cs.setFont(f, tam);
        cs.newLineAtOffset(px, py);
        cs.showText(limpiar(t));
        cs.endText();
    }

    private static void rect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void linea(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.setStrokingColor(LINEA);
        cs.setLineWidth(0.4f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private static String dinero(BigDecimal b) {
        return "$" + new DecimalFormat("#,##0.00", US).format(b);
    }
}
