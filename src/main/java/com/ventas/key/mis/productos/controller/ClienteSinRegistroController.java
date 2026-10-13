package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VerificarCorreoRequest;
import com.ventas.key.mis.productos.service.ClienteSinRegistroImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cliente sin registro", description = "Alta y verificacion de correo de cliente sin registro, previo a generar la venta")
@RestController
@RequestMapping("/v1/clientes-sin-registro")
@RequiredArgsConstructor
@Slf4j
public class ClienteSinRegistroController {

    private final ClienteSinRegistroImpl service;

    @Operation(summary = "Crear cliente sin registro", description = "Crea el registro antes de generar la venta, para poder verificar el correo si viene lleno.")
    @PostMapping
    public ResponseEntity<ResponseGeneric<ClienteSinRegistro>> crear(@RequestBody ClienteSinRegistroDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.crear(dto)));
    }

    @Operation(summary = "Enviar codigo de verificacion", description = "Genera un codigo de 6 digitos y lo envia al correo del registro. Falla si el registro no tiene correo.")
    @PostMapping("/{id}/enviar-codigo")
    public ResponseEntity<ResponseGeneric<String>> enviarCodigo(@PathVariable Integer id) {
        service.enviarCodigoVerificacion(id);
        return ResponseEntity.ok(new ResponseGeneric<>("Codigo enviado"));
    }

    @Operation(summary = "Verificar codigo", description = "Valida el codigo de 6 digitos. Si es correcto y no expiro, marca el correo como verificado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo verificado correctamente"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido o expirado")
    })
    @PostMapping("/{id}/verificar-codigo")
    public ResponseEntity<ResponseGeneric<String>> verificarCodigo(
            @PathVariable Integer id, @Valid @RequestBody VerificarCorreoRequest request) {
        service.verificarCodigo(id, request.getCodigo());
        return ResponseEntity.ok(new ResponseGeneric<>("Correo verificado correctamente"));
    }
}
