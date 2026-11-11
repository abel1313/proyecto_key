package com.ventas.key.hexagonal.flete.infraestructura.salida.archivo;

import com.ventas.key.hexagonal.flete.dominio.modelo.ReporteDeViaje;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * [Hexagonal: Driven Adapter] El reporte de un viaje en Excel (Apache POI): datos del viaje, cómo va el flete, los productos (compradas, vendidas,
 * quedan) y las ventas ligadas en el orden en que se recuperó el flete, con su fila de totales.
 */
@Component
public class ViajeExcelGenerador {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generar(ReporteDeViaje r, String negocio, String generadoPor) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet h = wb.createSheet("Viaje");
            h.setDisplayGridlines(false);
            Estilos e = new Estilos(wb);
            var v = r.viaje();
            int fila = 0;

            Row banda = h.createRow(fila++);
            banda.setHeightInPoints(28);
            for (int c = 0; c < 8; c++) banda.createCell(c).setCellStyle(e.banda);
            banda.getCell(0).setCellValue(negocio.toUpperCase());
            Row sub = h.createRow(fila++);
            for (int c = 0; c < 8; c++) sub.createCell(c).setCellStyle(e.bandaSub);
            sub.getCell(0).setCellValue("Reporte de viaje de compra · generado por " + generadoPor + " el " + java.time.LocalDateTime.now().format(DIA_HORA));
            fila++;

            fila = par(h, e, fila, "Viaje", v.nombre());
            fila = par(h, e, fila, "Fecha de la compra", v.fecha().format(DIA));
            if (v.nota() != null) fila = par(h, e, fila, "Nota", v.nota());
            fila = par(h, e, fila, "Estado", etiqueta(r.estado()));
            fila++;
            fila = parDinero(h, e, fila, "Flete total", v.fleteTotal());
            fila = parDinero(h, e, fila, "Otros gastos que cuentan para cubrir", v.costoACubrir().subtract(v.fleteTotal()));
            fila = parDinero(h, e, fila, "Costo a cubrir", v.costoACubrir());
            fila = parDinero(h, e, fila, "Flete recuperado con lo vendido", r.fleteRecuperado());
            fila = parDinero(h, e, fila, "Falta por cubrir", r.faltaPorCubrir());
            fila = parDinero(h, e, fila, "Flete de lo vendido después de cubrirlo (ya es ganancia)", r.fleteComoGanancia());
            if (r.pagadoEn() != null) fila = par(h, e, fila, "Flete cubierto el", r.pagadoEn().format(DIA) + " (venta #" + r.pagadoConVentaId() + ")");
            fila = par(h, e, fila, "Piezas compradas / vendidas / quedan", r.piezasCompradas() + " / " + r.piezasVendidas() + " / " + r.piezasRestantes());
            fila = parDinero(h, e, fila, "Total vendido", r.totalVendido());
            fila = parDinero(h, e, fila, "Ganancia de lo vendido (ya sin comisión de tarjeta)", r.gananciaVendida());
            fila++;

            // Productos
            fila = titulo(h, e, fila, "Productos del viaje");
            fila = encabezado(h, e, fila, "Producto", "Código de barras", "Compradas", "Vendidas", "Quedan", "Total vendido");
            for (var p : r.renglones()) {
                Row x = h.createRow(fila++);
                texto(x, 0, p.nombre() == null ? "Producto " + p.productoId() : p.nombre(), e.celda);
                texto(x, 1, p.codigoBarras() == null ? "" : p.codigoBarras(), e.celda);
                num(x, 2, p.piezasCompradas(), e.entero);
                num(x, 3, p.piezasVendidas(), e.entero);
                num(x, 4, p.piezasRestantes(), e.entero);
                num(x, 5, p.totalVendido().doubleValue(), e.dinero);
            }
            fila++;

            // Ventas ligadas
            fila = titulo(h, e, fila, "Ventas ligadas a este viaje (en el orden en que se recuperó el flete)");
            fila = encabezado(h, e, fila, "Venta", "Fecha", "Producto", "Piezas", "Flete por pieza", "Flete recuperado", "Flete ya ganancia", "Total vendido");
            BigDecimal rec = BigDecimal.ZERO, gan = BigDecimal.ZERO, tot = BigDecimal.ZERO;
            int piezas = 0;
            java.util.Map<Integer, String> nombres = new java.util.HashMap<>();
            r.renglones().forEach(p -> nombres.put(p.productoId(), p.nombre()));
            for (var l : r.lineas()) {
                var vl = l.venta();
                Row x = h.createRow(fila++);
                texto(x, 0, "#" + vl.ventaId(), e.celda);
                texto(x, 1, vl.fechaVenta() == null ? "" : vl.fechaVenta().format(DIA), e.celda);
                texto(x, 2, nombres.getOrDefault(vl.productoId(), "Producto " + vl.productoId()), e.celda);
                num(x, 3, vl.cantidad(), e.entero);
                num(x, 4, vl.fletePorPieza().doubleValue(), e.dinero);
                num(x, 5, l.fleteRecuperado().doubleValue(), e.dinero);
                num(x, 6, l.fleteComoGanancia().doubleValue(), e.dinero);
                num(x, 7, vl.totalVendido().doubleValue(), e.dinero);
                piezas += vl.cantidad();
                rec = rec.add(l.fleteRecuperado());
                gan = gan.add(l.fleteComoGanancia());
                tot = tot.add(vl.totalVendido());
            }
            Row t = h.createRow(fila++);
            texto(t, 0, "TOTALES", e.totalTexto);
            for (int c = 1; c <= 7; c++) t.createCell(c).setCellStyle(e.totalTexto);
            num(t, 3, piezas, e.totalEntero);
            num(t, 5, rec.doubleValue(), e.totalDinero);
            num(t, 6, gan.doubleValue(), e.totalDinero);
            num(t, 7, tot.doubleValue(), e.totalDinero);

            double[] anchos = {34, 22, 30, 12, 16, 18, 18, 16};
            for (int c = 0; c < anchos.length; c++) h.setColumnWidth(c, (int) (anchos[c] * 256));
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    static String etiqueta(String estado) {
        return switch (estado) {
            case ReporteDeViaje.VENDIDO_POR_COMPLETO -> "Vendido por completo";
            case ReporteDeViaje.FLETE_CUBIERTO -> "Flete cubierto";
            default -> "Abierto";
        };
    }

    private int titulo(XSSFSheet h, Estilos e, int fila, String texto) {
        Row r = h.createRow(fila++);
        texto(r, 0, texto, e.titulo);
        return fila;
    }

    private int encabezado(XSSFSheet h, Estilos e, int fila, String... cols) {
        Row r = h.createRow(fila++);
        r.setHeightInPoints(20);
        for (int c = 0; c < cols.length; c++) texto(r, c, cols[c], e.encabezado);
        return fila;
    }

    private int par(XSSFSheet h, Estilos e, int fila, String clave, String valor) {
        Row r = h.createRow(fila++);
        texto(r, 0, clave, e.clave);
        texto(r, 1, valor, e.valor);
        return fila;
    }

    private int parDinero(XSSFSheet h, Estilos e, int fila, String clave, BigDecimal valor) {
        Row r = h.createRow(fila++);
        texto(r, 0, clave, e.clave);
        num(r, 1, valor.doubleValue(), e.dinero);
        return fila;
    }

    private static void texto(Row r, int c, String t, XSSFCellStyle s) {
        Cell x = r.createCell(c);
        x.setCellValue(t);
        x.setCellStyle(s);
    }

    private static void num(Row r, int c, double v, XSSFCellStyle s) {
        Cell x = r.createCell(c);
        x.setCellValue(v);
        x.setCellStyle(s);
    }

    /** Los estilos del libro (un solo juego, para no pasar del límite de estilos de Excel). */
    private static final class Estilos {
        final XSSFCellStyle banda, bandaSub, titulo, encabezado, clave, valor, celda, entero, dinero, totalTexto, totalEntero, totalDinero;

        Estilos(XSSFWorkbook wb) {
            XSSFColor verde = new XSSFColor(new byte[]{0x00, (byte) 0x87, 0x5A}, null);
            XSSFColor suave = new XSSFColor(new byte[]{(byte) 0xE6, (byte) 0xF4, (byte) 0xEE}, null);
            XSSFColor blanco = new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null);
            banda = base(wb, verde, blanco, 16, true, false);
            bandaSub = base(wb, verde, blanco, 10, false, false);
            titulo = base(wb, null, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 12, true, false);
            encabezado = base(wb, verde, blanco, 10, true, false);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            clave = base(wb, null, new XSSFColor(new byte[]{0x6B, 0x72, (byte) 0x80}, null), 10, false, false);
            valor = base(wb, null, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, true, false);
            celda = base(wb, null, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, false, true);
            entero = base(wb, null, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, false, true);
            entero.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            entero.setAlignment(HorizontalAlignment.RIGHT);
            dinero = base(wb, null, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, false, true);
            dinero.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
            dinero.setAlignment(HorizontalAlignment.RIGHT);
            totalTexto = base(wb, suave, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, true, true);
            totalEntero = base(wb, suave, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, true, true);
            totalEntero.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            totalEntero.setAlignment(HorizontalAlignment.RIGHT);
            totalDinero = base(wb, suave, new XSSFColor(new byte[]{0x1F, 0x29, 0x37}, null), 10, true, true);
            totalDinero.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
            totalDinero.setAlignment(HorizontalAlignment.RIGHT);
        }

        private static XSSFCellStyle base(XSSFWorkbook wb, XSSFColor fondo, XSSFColor letra, int tam, boolean negrita, boolean borde) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setFontHeightInPoints((short) tam);
            f.setBold(negrita);
            f.setColor(letra);
            s.setFont(f);
            if (fondo != null) {
                s.setFillForegroundColor(fondo);
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (borde) {
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(new byte[]{(byte) 0xD5, (byte) 0xD9, (byte) 0xE0}, null));
            }
            s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            return s;
        }
    }
}
