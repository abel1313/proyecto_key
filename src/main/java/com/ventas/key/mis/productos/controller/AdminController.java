package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Administracion", description = "Operaciones administrativas: gestion de cache Redis y pruebas internas")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CacheManager cacheManager;
    private final RabbitTemplate rabbitTemplate;

    @Operation(summary = "Prueba de RabbitMQ", description = "Endpoint temporal para verificar conectividad con RabbitMQ publicando un mensaje de prueba. ELIMINAR en produccion.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mensaje publicado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/test-rabbit")
    public ResponseEntity<ResponseGeneric<String>> testRabbit() {
        RequestProductoImagen mensaje = new RequestProductoImagen();
        mensaje.setProductoId(999);
        mensaje.setImagenId(1L);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,
                RabbitMQConfig.ROUTING_KEY_GUARDAR,
                List.of(mensaje)
        );
        log.info("Mensaje de prueba publicado a RabbitMQ");
        return ResponseEntity.ok(new ResponseGeneric<>("Mensaje publicado a Rabbit correctamente"));
    }

    @Operation(summary = "Limpiar cache Redis", description = "Elimina todas las entradas de todas las caches Redis activas. Requiere autenticacion de administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache limpiada correctamente; devuelve lista de caches afectadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @DeleteMapping("/cache")
    public ResponseEntity<ResponseGeneric<List<String>>> limpiarCache() {
        List<String> limpiadas = new ArrayList<>();
        cacheManager.getCacheNames().forEach(nombre -> {
            Cache cache = cacheManager.getCache(nombre);
            if (cache != null) {
                cache.clear();
                limpiadas.add(nombre);
            }
        });
        log.info("Cache Redis limpiada. Caches afectadas: {}", limpiadas);
        return ResponseEntity.ok(new ResponseGeneric<List<String>>(limpiadas));
    }
}