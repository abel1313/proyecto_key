package com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReporteCalculado;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.TotalesReporte;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.ColumnasReporte.Columna;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] El reporte en Excel (Apache POI): banda con el nombre del negocio, datos de contacto, quién y cuándo lo generó, el
 * periodo y los filtros, tarjetas con los totales, y la tabla con encabezado verde, filas alternadas, rojo si hay pérdida, «SIN FLETE» en ámbar,
 * filtros en el encabezado, panel congelado y la fila de totales al final. Listo para imprimir (horizontal, ajustado al ancho, encabezado repetido
 * y pie con número de página).
 */
public class ExcelReporteGenerador {

    private static final int VERDE = 0x00875A, VERDE_SUAVE = 0xE6F4EE, GRIS_FILA = 0xF6F7F9, TEXTO = 0x1F2937, SUAVE = 0x6B7280,
            ROJO = 0xB91C1C, AMBAR = 0xB45309, BLANCO = 0xFFFFFF, LINEA = 0xD5D9E0;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generar(ReporteCalculado r, EncabezadoReporte enc) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet hoja = wb.createSheet("Reporte");
            hoja.setDisplayGridlines(false);
            Estilos est = new Estilos(wb);
            List<Columna> cols = ColumnasReporte.COLUMNAS;
            int n = cols.size();
            int ult = n - 1;

            // ── Banda con el negocio ──
            Row b0 = hoja.createRow(0);
            b0.setHeightInPoints(30);
            Row b1 = hoja.createRow(1);
            b1.setHeightInPoints(18);
            for (int c = 0; c < n; c++) {
                b0.createCell(c).setCellStyle(est.banda(18, true));
                b1.createCell(c).setCellStyle(est.banda(11, false));
            }
            b0.getCell(0).setCellValue(enc.negocio().toUpperCase(Locale.ROOT));
            b1.getCell(0).setCellValue(EncabezadoReporte.TITULO);
            b0.getCell(ult - 3).setCellValue("Generado el " + FECHA_HORA.format(enc.generado()));
            if (enc.generadoPor() != null && !enc.generadoPor().isBlank()) b1.getCell(ult - 3).setCellValue("Por: " + enc.generadoPor());
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
            hoja.addMergedRegion(new CellRangeAddress(0, 0, ult - 3, ult));
            hoja.addMergedRegion(new CellRangeAddress(1, 1, ult - 3, ult));
            b0.getCell(ult - 3).setCellStyle(est.banda(9, true, HorizontalAlignment.RIGHT));
            b1.getCell(ult - 3).setCellStyle(est.banda(9, false, HorizontalAlignment.RIGHT));

            int fila = 2;
            if (!enc.lineaContacto().isEmpty()) {
                Row c = hoja.createRow(fila++);
                Cell cell = c.createCell(0);
                cell.setCellValue(enc.lineaContacto());
                cell.setCellStyle(est.texto(9, false, SUAVE));
            }
            Row filtros = hoja.createRow(fila++);
            Cell fc = filtros.createCell(0);
            fc.setCellValue(ColumnasReporte.descripcion(r.filtro()));
            fc.setCellStyle(est.texto(11, true, TEXTO));
            filtros.setHeightInPoints(20);
            fila++;   // renglón en blanco

            // ── Tarjetas con los totales (cada una ocupa 3 columnas) ──
            TotalesReporte t = r.totales();
            String[] etq = {"PRODUCTOS", "PIEZAS VENDIDAS", "TOTAL VENDIDO", "GANANCIA VENDIDA", "GANANCIA POR VENDER", "GANANCIA TOTAL"};
            Object[] val = {(double) t.productos(), (double) t.piezas(), t.total(), t.gananciaVendida(), t.gananciaPorVender(), t.gananciaTotal()};
            String[] sub = {t.productosVendidos() + " vendidos · " + t.productosSinVender() + " sin vender", t.piezasPorVender() + " en stock por vender",
                    "Costo $" + String.format(Locale.US, "%,.2f", t.costo()), t.margenRealPct() == null ? "" : String.format(Locale.US, "%.2f%% real", t.margenRealPct()),
                    "Si se vende todo el stock", "Vendida + por vender"};
            Row rEtq = hoja.createRow(fila), rVal = hoja.createRow(fila + 1), rSub = hoja.createRow(fila + 2);
            rVal.setHeightInPoints(26);
            for (int i = 0; i < 6; i++) {
                // La columna A (nombre) es ancha: la tarjeta de productos va sola ahí; las demás ocupan 3 columnas cada una.
                int c0 = i == 0 ? 0 : 1 + (i - 1) * 3;
                int c1 = i == 0 ? 0 : c0 + 2;
                boolean verde = i == 3 || i == 5;
                for (int c = c0; c <= c1 && c <= ult; c++) {
                    rEtq.createCell(c).setCellStyle(est.tarjeta(8, true, SUAVE, false, false));
                    rVal.createCell(c).setCellStyle(est.tarjeta(15, true, verde ? VERDE : TEXTO, true, false));
                    rSub.createCell(c).setCellStyle(est.tarjeta(8, false, SUAVE, false, true));
                }
                rEtq.getCell(c0).setCellValue(etq[i]);
                Cell v = rVal.getCell(c0);
                v.setCellValue(val[i] instanceof BigDecimal bd ? bd.doubleValue() : (Double) val[i]);
                if (i >= 2) v.setCellStyle(est.tarjetaDinero(verde ? (((BigDecimal) val[i]).signum() < 0 ? ROJO : VERDE) : TEXTO));
                else v.setCellStyle(est.tarjetaEntero());
                rSub.getCell(c0).setCellValue(sub[i]);
                if (c1 > c0) {
                    hoja.addMergedRegion(new CellRangeAddress(fila, fila, c0, Math.min(c1, ult)));
                    hoja.addMergedRegion(new CellRangeAddress(fila + 1, fila + 1, c0, Math.min(c1, ult)));
                    hoja.addMergedRegion(new CellRangeAddress(fila + 2, fila + 2, c0, Math.min(c1, ult)));
                }
            }
            fila += 4;   // 3 renglones de tarjetas + 1 en blanco

            // ── Tabla ──
            int filaTitulos = fila;
            Row h = hoja.createRow(fila++);
            h.setHeightInPoints(30);
            for (int c = 0; c < n; c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(cols.get(c).titulo());
                cell.setCellStyle(est.encabezado());
            }
            int primera = fila;
            int k = 0;
            for (FilaReporte f : r.filas()) {
                Row row = hoja.createRow(fila++);
                boolean alt = k++ % 2 == 1;
                boolean sinVentas = f.base().piezas() == 0;
                for (int c = 0; c < n; c++) {
                    Columna col = cols.get(c);
                    Object v = col.valor().apply(f);
                    Cell cell = row.createCell(c);
                    boolean rojo = v instanceof BigDecimal b && col.tipo() == ColumnasReporte.Tipo.DINERO && b.signum() < 0;
                    boolean ambar = col.titulo().equals("¿Flete?") && f.sinFlete();
                    int color = rojo ? ROJO : ambar ? AMBAR : sinVentas ? SUAVE : TEXTO;
                    escribir(cell, v);
                    cell.setCellStyle(est.celda(col.tipo(), alt, color, false));
                }
            }
            int ultimaDato = fila - 1;
            Row tot = hoja.createRow(fila);
            for (int c = 0; c < n; c++) {
                Columna col = cols.get(c);
                Cell cell = tot.createCell(c);
                escribir(cell, col.total().apply(r.totales()));
                cell.setCellStyle(est.celda(col.tipo(), false, TEXTO, true));
            }
            tot.setHeightInPoints(20);

            // ── Presentación ──
            if (ultimaDato >= primera) hoja.setAutoFilter(new CellRangeAddress(filaTitulos, ultimaDato, 0, ult));
            hoja.createFreezePane(1, filaTitulos + 1);
            for (int c = 0; c < n; c++) hoja.setColumnWidth(c, c == 0 ? 11500 : c == 1 ? 5000 : c == 2 ? 3800 : 3900);
            hoja.getPrintSetup().setLandscape(true);
            hoja.getPrintSetup().setPaperSize(PrintSetup.A3_PAPERSIZE);
            hoja.setFitToPage(true);
            hoja.getPrintSetup().setFitWidth((short) 1);
            hoja.getPrintSetup().setFitHeight((short) 0);
            hoja.setRepeatingRows(new CellRangeAddress(filaTitulos, filaTitulos, -1, -1));
            Footer pie = hoja.getFooter();
            pie.setLeft(enc.negocio() + " · " + EncabezadoReporte.TITULO + " · Uso interno");
            pie.setRight("Página &P de &N");
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el Excel del reporte.", e);
        }
    }

    private static void escribir(Cell cell, Object v) {
        if (v == null) return;
        if (ColumnasReporte.esNumero(v)) cell.setCellValue(v instanceof BigDecimal b ? b.doubleValue() : ((Number) v).doubleValue());
        else cell.setCellValue(String.valueOf(v));
    }

    /** Fábrica de estilos con caché: Excel limita cuántos estilos distintos acepta, así que se reutilizan. */
    private static final class Estilos {
        private final XSSFWorkbook wb;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();
        private final Map<String, XSSFFont> fuentes = new HashMap<>();

        Estilos(XSSFWorkbook wb) { this.wb = wb; }

        private static XSSFColor rgb(int c) {
            return new XSSFColor(new byte[]{(byte) (c >> 16), (byte) (c >> 8), (byte) c}, null);
        }

        private XSSFFont fuente(int tam, boolean negrita, int color) {
            return fuentes.computeIfAbsent(tam + "|" + negrita + "|" + color, k -> {
                XSSFFont f = wb.createFont();
                f.setFontHeightInPoints((short) tam);
                f.setBold(negrita);
                f.setColor(rgb(color));
                return f;
            });
        }

        private XSSFCellStyle base(String clave, java.util.function.Consumer<XSSFCellStyle> arma) {
            return cache.computeIfAbsent(clave, k -> {
                XSSFCellStyle s = wb.createCellStyle();
                s.setVerticalAlignment(VerticalAlignment.CENTER);
                arma.accept(s);
                return s;
            });
        }

        private static void fondo(XSSFCellStyle s, int color) {
            s.setFillForegroundColor(rgb(color));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        XSSFCellStyle banda(int tam, boolean negrita) { return banda(tam, negrita, HorizontalAlignment.LEFT); }

        XSSFCellStyle banda(int tam, boolean negrita, HorizontalAlignment al) {
            return base("banda" + tam + negrita + al, s -> { fondo(s, VERDE); s.setFont(fuente(tam, negrita, BLANCO)); s.setAlignment(al); s.setIndention((short) 1); });
        }

        XSSFCellStyle texto(int tam, boolean negrita, int color) {
            return base("texto" + tam + negrita + color, s -> { s.setFont(fuente(tam, negrita, color)); s.setIndention((short) 1); });
        }

        XSSFCellStyle tarjeta(int tam, boolean negrita, int color, boolean valor, boolean pie) {
            return base("tarj" + tam + negrita + color + valor + pie, s -> {
                fondo(s, VERDE_SUAVE);
                s.setFont(fuente(tam, negrita, color));
                s.setAlignment(HorizontalAlignment.LEFT);
                s.setIndention((short) 1);
            });
        }

        XSSFCellStyle tarjetaDinero(int color) {
            return base("tdin" + color, s -> {
                fondo(s, VERDE_SUAVE);
                s.setFont(fuente(15, true, color));
                s.setAlignment(HorizontalAlignment.LEFT);
                s.setIndention((short) 1);
                s.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
            });
        }

        XSSFCellStyle tarjetaEntero() {
            return base("tent", s -> {
                fondo(s, VERDE_SUAVE);
                s.setFont(fuente(15, true, TEXTO));
                s.setAlignment(HorizontalAlignment.LEFT);
                s.setIndention((short) 1);
                s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            });
        }

        XSSFCellStyle encabezado() {
            return base("enc", s -> {
                fondo(s, VERDE);
                s.setFont(fuente(10, true, BLANCO));
                s.setAlignment(HorizontalAlignment.CENTER);
                s.setWrapText(true);
                s.setBorderBottom(BorderStyle.THIN);
            });
        }

        XSSFCellStyle celda(ColumnasReporte.Tipo tipo, boolean alt, int color, boolean total) {
            return base("cel" + tipo + alt + color + total, s -> {
                if (total) fondo(s, VERDE_SUAVE); else if (alt) fondo(s, GRIS_FILA);
                s.setFont(fuente(total ? 10 : 9, total, color));
                s.setBorderBottom(BorderStyle.HAIR);
                s.setBottomBorderColor(rgb(LINEA));
                if (total) { s.setBorderTop(BorderStyle.MEDIUM); s.setTopBorderColor(rgb(VERDE)); }
                switch (tipo) {
                    case DINERO -> { s.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00")); s.setAlignment(HorizontalAlignment.RIGHT); }
                    case PORCENTAJE -> { s.setDataFormat(wb.createDataFormat().getFormat("0.00\"%\"")); s.setAlignment(HorizontalAlignment.RIGHT); }
                    case ENTERO -> { s.setDataFormat(wb.createDataFormat().getFormat("#,##0")); s.setAlignment(HorizontalAlignment.RIGHT); }
                    default -> { s.setAlignment(HorizontalAlignment.LEFT); s.setIndention((short) 1); }
                }
            });
        }
    }
}
