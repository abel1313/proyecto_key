package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.dto.ResultadoCargaDto;
import org.springframework.web.multipart.MultipartFile;

public interface ISubirDocumentosService {
    ResultadoCargaDto procesarExcelProductos(MultipartFile archivo);
}