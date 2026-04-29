package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.ResultadoCargaDto;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.ISubirDocumentosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubirDocumentosServiceImpl implements ISubirDocumentosService {

    private static final List<String> LIST_HEADER_PRODUCTO = List.of(
            "codigoBarras", "nombreProducto", "piezas", "precioCompra",
            "PrecioVenta", "precioDescuento", "piezas", "descripcion",
            "stock", "color", "marca", "contenidoNeta"
    );

    private static final List<String> LIST_HEADER_VARIANTES = List.of(
            "talla", "color", "presentacion", "marca", "stock", "contenidoNeta", "descripcion"
    );

    // Índices dentro de LIST_HEADER_PRODUCTO
    private static final int IDX_CODIGO_BARRAS  = 0;
    private static final int IDX_NOMBRE         = 1;
    private static final int IDX_PIEZAS         = 2;
    private static final int IDX_PRECIO_COSTO   = 3;
    private static final int IDX_PRECIO_VENTA   = 4;
    private static final int IDX_PRECIO_REBAJA  = 5;
    private static final int IDX_DESCRIPCION    = 7;
    private static final int IDX_STOCK          = 8;
    private static final int IDX_COLOR          = 9;
    private static final int IDX_MARCA          = 10;
    private static final int IDX_CONTENIDO      = 11;

    private final ICodigoBarrasRepository codigoBarrasRepository;
    private final IProductosRepository productosRepository;
    private final IVarianteRepository varianteRepository;

    @Override
    @Transactional
    public ResultadoCargaDto procesarExcelProductos(MultipartFile archivo) {
        validarExtension(archivo);

        int insertados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row fila = sheet.getRow(i);
                if (fila == null || esFilaVacia(fila)) continue;

                try {
                    String codigoBarras = leerTexto(fila, IDX_CODIGO_BARRAS);
                    if (codigoBarras == null || codigoBarras.isBlank()) {
                        errores.add("Fila " + (i + 1) + ": código de barras vacío, se omite.");
                        omitidos++;
                        continue;
                    }

                    if (codigoBarrasRepository.findByCodigoBarras(codigoBarras).isPresent()) {
                        log.info("Fila {}: código de barras '{}' ya existe, se omite.", i + 1, codigoBarras);
                        omitidos++;
                        continue;
                    }

                    Double stockDouble = leerNumerico(fila, IDX_STOCK);
                    int stock = (stockDouble != null && stockDouble > 0) ? stockDouble.intValue() : 1;

                    CodigoBarra cb = new CodigoBarra();
                    cb.setCodigoBarras(codigoBarras);
                    cb = codigoBarrasRepository.save(cb);

                    Producto producto = new Producto();
                    producto.setCodigoBarras(cb);
                    producto.setNombre(leerTexto(fila, IDX_NOMBRE));
                    producto.setPiezas(leerNumerico(fila, IDX_PIEZAS));
                    producto.setPrecioCosto(leerNumerico(fila, IDX_PRECIO_COSTO));
                    producto.setPrecioVenta(leerNumerico(fila, IDX_PRECIO_VENTA));
                    producto.setPrecioRebaja(leerNumerico(fila, IDX_PRECIO_REBAJA));
                    producto.setDescripcion(leerTexto(fila, IDX_DESCRIPCION));
                    producto.setStock(stock);
                    producto.setColor(leerTexto(fila, IDX_COLOR));
                    producto.setMarca(leerTexto(fila, IDX_MARCA));
                    producto.setContenido(leerTexto(fila, IDX_CONTENIDO));
                    producto.setHabilitado('1');
                    producto = productosRepository.save(producto);

                    for (int j = 0; j < stock; j++) {
                        Variantes variante = new Variantes();
                        variante.setProducto(producto);
                        variante.setColor(producto.getColor());
                        variante.setMarca(producto.getMarca());
                        variante.setContenidoNeto(producto.getContenido());
                        variante.setDescripcion(producto.getDescripcion());
                        variante.setStock(1);
                        variante.setTalla(null);
                        variante.setPresentacion(null);
                        varianteRepository.save(variante);
                    }

                    log.info("Fila {}: producto '{}' insertado con {} variante(s).", i + 1, producto.getNombre(), stock);
                    insertados++;

                } catch (Exception e) {
                    log.error("Error procesando fila {}: {}", i + 1, e.getMessage());
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage());
        }

        return ResultadoCargaDto.builder()
                .insertados(insertados)
                .omitidos(omitidos)
                .errores(errores)
                .build();
    }

    private void validarExtension(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null ||
                (!nombre.toLowerCase().endsWith(".xls") && !nombre.toLowerCase().endsWith(".xlsx"))) {
            throw new IllegalArgumentException("Solo se aceptan archivos .xls o .xlsx");
        }
    }

    private boolean esFilaVacia(Row fila) {
        for (Cell celda : fila) {
            if (celda.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String leerTexto(Row fila, int columna) {
        Cell celda = fila.getCell(columna, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (celda == null) return null;

        CellType tipo = celda.getCellType() == CellType.FORMULA
                ? celda.getCachedFormulaResultType()
                : celda.getCellType();

        switch (tipo) {
            case NUMERIC -> { return String.valueOf((long) celda.getNumericCellValue()); }
            case STRING  -> {
                String val = celda.getStringCellValue().trim();
                return val.isBlank() ? null : val;
            }
            default -> { return null; }
        }
    }

    private Double leerNumerico(Row fila, int columna) {
        Cell celda = fila.getCell(columna, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (celda == null) return null;

        CellType tipo = celda.getCellType() == CellType.FORMULA
                ? celda.getCachedFormulaResultType()
                : celda.getCellType();

        switch (tipo) {
            case NUMERIC -> { return celda.getNumericCellValue(); }
            case STRING  -> {
                try {
                    String val = celda.getStringCellValue().trim();
                    return val.isBlank() ? null : Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            default -> { return null; }
        }
    }
}