package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.controller.api.IControllerGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CrudAbstractServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractController<
        Response,
        OptionalResponse extends Optional<Response>,    // Warning tolerable: Optional es final, bound requerido por CrudAbstractServiceImpl
        ListResponse extends List<Response>,
        TipoDato,
        Paginacion extends PginaDto<List<Response>>,
        ServiceG extends CrudAbstractServiceImpl<
                Response,
                ListResponse,
                OptionalResponse,
                TipoDato,
                Paginacion>>
        implements IControllerGenerico<
        Response,
        OptionalResponse,
        ListResponse,
        TipoDato> {

    protected final ServiceG sGenerico;

    protected AbstractController(final ServiceG sGenerico) {
        this.sGenerico = sGenerico;
    }

    @Operation(summary = "Eliminar registro", description = "Elimina el registro identificado por el cuerpo de la solicitud.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro eliminado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/delete")
    @Override
    public ResponseEntity<ResponseGeneric<Response>> delete(@RequestBody TipoDato requestG) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(this.sGenerico.delete(requestG)));   // [5] ResponseEntity.ok() en lugar de .status(HttpStatus.OK).body()
        } catch (Exception e) {
            log.error("Error al eliminar: {}", e.getMessage(), e);                              // [2] log.error con la excepción completa (conserva el stack trace)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<>(null, "Error al eliminar el registro"));        // [2] respuesta de error real en lugar de return null
        }
    }

    @Operation(summary = "Listar registros paginados", description = "Retorna una pagina de registros. El parametro page es base-0.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/getAll")
    @Override
    public ResponseEntity<ResponseGeneric<ListResponse>> findAll(
            @Parameter(description = "Numero de pagina (base 0)") @RequestParam int page,
            @Parameter(description = "Registros por pagina") @RequestParam int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<ListResponse>(this.sGenerico.findAll(page, size)));
        } catch (Exception e) {
            log.error("Error al obtener listado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<ListResponse>((ListResponse)null, "Error al obtener el listado"));
        }
    }

    @Operation(summary = "Buscar registro por identificador", description = "Retorna el registro cuyo identificador coincide con tipoDato.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro encontrado"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/getOne/{tipoDato}")
    @Override
    public ResponseEntity<ResponseGeneric<OptionalResponse>> findBy(
            @Parameter(description = "Identificador del registro") @PathVariable TipoDato tipoDato) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(this.sGenerico.findById(tipoDato)));   // [6] eliminadas variables intermedias innecesarias
        } catch (Exception e) {
            log.error("Error al buscar id {}: {}", tipoDato, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<>((OptionalResponse) null, "Error al buscar el registro"));
        }
    }

    @Operation(summary = "Crear nuevo registro", description = "Guarda un nuevo registro tras validar los campos anotados con @Validated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/save")
    @Override
    public ResponseEntity<ResponseGeneric<Response>> save(
            @Validated @RequestBody Response requestG, BindingResult result) {
        try {
            return ejecutarGuardado(requestG, result);                                            // [4] delegado al método privado, eliminada la duplicación
        } catch (Exception e) {
            log.error("Error al guardar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<>(null, "Error al guardar el registro"));
        }
    }

    @Operation(summary = "Actualizar registro existente", description = "Actualiza el registro identificado por tipoDato con los nuevos datos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @Override
    @PutMapping("/update/{tipoDato}")
    public ResponseEntity<ResponseGeneric<Response>> update(
            @Parameter(description = "Identificador del registro a actualizar") @PathVariable TipoDato tipoDato,
            @Validated @RequestBody Response requestG,
            BindingResult result) throws Exception {
        try {
            return ejecutarGuardado(requestG, result);                                            // [4] delegado al método privado, eliminada la duplicación
        } catch (Exception e) {
            log.error("Error al actualizar id {}: {}", tipoDato, e.getMessage(), e);             // [2] log conservando stack trace en lugar de throw new Exception(e.getMessage())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseGeneric<>(null, "Error al actualizar el registro"));
        }
    }

    // Lógica común de save y update extraída aquí para no duplicarla    [4]
    private ResponseEntity<ResponseGeneric<Response>> ejecutarGuardado(
            Response requestG, BindingResult result) throws Exception {
        if (result.hasErrors()) {
            ResponseGeneric<Response> errorResponse = new ResponseGeneric<>((Response) null);
            errorResponse.setMensaje(getErroresGeneric(result));                                  // [7] reutiliza getErroresGeneric() en lugar de duplicar el stream
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        return ResponseEntity.ok(new ResponseGeneric<>(this.sGenerico.save(requestG)));
    }

    private String getErroresGeneric(BindingResult result) {
        return result.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }
}
