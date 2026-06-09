package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Administracion", description = "Operaciones administrativas: gestion de cache Redis y pruebas internas")
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CacheService cacheService;

    @Operation(summary = "Limpiar cache Redis", description = "Elimina todas las entradas de todas las caches Redis activas. Requiere autenticacion de administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache limpiada correctamente; devuelve lista de caches afectadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @DeleteMapping("/cache")
    public ResponseEntity<ResponseGeneric<List<String>>> limpiarCache() {
        List<String> limpiadas = cacheService.evictAll();
        return ResponseEntity.ok(new ResponseGeneric<List<String>>(limpiadas));
    }
}