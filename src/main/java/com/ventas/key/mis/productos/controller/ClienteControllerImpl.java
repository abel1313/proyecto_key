package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Direccion;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VerificarCorreoRequest;
import com.ventas.key.mis.productos.service.ClienteServiceImpl;
import com.ventas.key.mis.productos.service.UsuarioDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Clientes", description = "CRUD de clientes con busqueda paginada por nombre")
@RestController
@RequestMapping("/v1/clientes")
@Slf4j
public class ClienteControllerImpl extends AbstractController<
        Cliente,
        Optional<Cliente>,
        List<Cliente>,
        Integer,
        PginaDto<List<Cliente>>,
        ClienteServiceImpl> {

    private final UsuarioDetailsService usuarioDetailsService;

    public ClienteControllerImpl(ClienteServiceImpl sGenerico, UsuarioDetailsService usuarioDetailsService) {
        super(sGenerico);
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Operation(summary = "Crear o actualizar cliente", description = "Guarda el cliente vinculado al usuario indicado. Si el usuario ya tiene cliente, actualiza ese registro. Las direcciones se asocian automaticamente al cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente guardado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @Override
    public ResponseEntity<ResponseGeneric<Cliente>> save(Cliente requestG, BindingResult result) {
        Optional<Usuario> usr = this.usuarioDetailsService.findById(requestG.getUsuario().getId().intValue());
        Cliente existente = null;
        if (usr.isPresent()) {
            requestG.setUsuario(usr.get());
            if (usr.get().getCliente() != null && usr.get().getCliente().getId() != null) {
                requestG.setId(usr.get().getCliente().getId());
                existente = usr.get().getCliente();
            }
        }

        // El guardado generico hace merge() del objeto completo (repository.save) — cualquier
        // campo administrado por el back que el front no mande en el JSON se pisaria con el
        // default de la clase (false/null). Hay que preservarlos explicitamente (mejora 12/15).
        boolean disparaVerificacionCorreoNuevo = false;
        if (existente != null) {
            requestG.setCodigoVerificacion(existente.getCodigoVerificacion());
            requestG.setCodigoVerificacionExpira(existente.getCodigoVerificacionExpira());

            String correoNuevo = requestG.getCorreoElectronico();
            String correoActual = existente.getCorreoElectronico();
            boolean cambioDeCorreo = correoNuevo != null && !correoNuevo.equalsIgnoreCase(correoActual);
            if (cambioDeCorreo && !AuthenticationUtils.isAdminContext()) {
                // Mejora 15: el correo nuevo NO se aplica de inmediato — queda pendiente de
                // verificar. El correo actual (ya verificado) sigue siendo el vigente.
                // Un ADMIN editando al cliente queda fuera de esta regla (aplica directo, sin
                // pedir verificacion) — decisión explícita del diseño, mejora 15 punto 12.
                requestG.setCorreoElectronico(correoActual);
                requestG.setCorreoPendiente(correoNuevo);
                requestG.setCorreoVerificado(existente.getCorreoVerificado());
                disparaVerificacionCorreoNuevo = true;
            } else if (cambioDeCorreo) {
                // Admin cambiando el correo: se aplica directo y queda verificado (confía en el
                // admin), sin dejar nada pendiente.
                requestG.setCorreoVerificado(true);
                requestG.setCorreoPendiente(null);
            } else {
                requestG.setCorreoPendiente(existente.getCorreoPendiente());
                requestG.setCorreoVerificado(existente.getCorreoVerificado());
            }
        }

        Set<Direccion> direcciones = Optional.ofNullable(requestG.getListDirecciones())
                .orElse(Set.of())
                .stream()
                .map(mpa -> {
                    Direccion direccion = new Direccion();
                    direccion.setCalle(mpa.getCalle());
                    direccion.setColonia(mpa.getColonia());
                    direccion.setMunicipio(mpa.getMunicipio());
                    direccion.setReferencias(mpa.getReferencias());
                    direccion.setCodigoPostal(mpa.getCodigoPostal());
                    direccion.setPredefinida(mpa.isPredefinida());
                    direccion.setCliente(requestG);
                    return direccion;
                })
                .collect(Collectors.toSet());

        requestG.setListDirecciones(direcciones);
        ResponseEntity<ResponseGeneric<Cliente>> response = super.save(requestG, result);

        if (disparaVerificacionCorreoNuevo && requestG.getId() != null) {
            try {
                sGenerico.enviarCodigoVerificacionCorreo(requestG.getId());
            } catch (Exception e) {
                log.warn("No se pudo enviar el codigo de verificacion tras cambio de correo de clienteId={}: {}",
                        requestG.getId(), e.getMessage());
            }
        }
        return response;
    }

    @Operation(summary = "Buscar cliente por ID de cliente", description = "Retorna el cliente cuyo ID coincide con el parametro idCliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("buscarPorIdCliente/{idCliente}")
    public ResponseEntity<ResponseGeneric<Optional<Cliente>>> findByIdCliente(
            @Parameter(description = "ID del cliente") @PathVariable int idCliente) {
        return ResponseEntity.status(HttpStatus.OK).body(sGenerico.findClienteById(idCliente));
    }

    @Operation(summary = "Buscar clientes por nombre (paginado)", description = "Retorna una pagina de clientes cuyo nombre contiene el texto buscado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes encontrados"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PageableDto<List<ClienteBusquedaDto>>>> buscarClientes(
            @Parameter(description = "Texto a buscar en el nombre del cliente") @RequestParam String nombre,
            @Parameter(description = "Numero de pagina (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Registros por pagina") @RequestParam(defaultValue = "10") int size) {
        try {
            PageableDto<List<ClienteBusquedaDto>> resultado = sGenerico.buscarClientes(nombre, page, size);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(resultado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Enviar codigo de verificacion de correo", description = "Genera un codigo de 6 digitos (expira en 15 minutos) y lo envia al correo registrado del cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Codigo enviado"),
        @ApiResponse(responseCode = "400", description = "Cliente no encontrado o sin correo registrado")
    })
    @PostMapping("/{id}/enviar-codigo-verificacion")
    public ResponseEntity<ResponseGeneric<String>> enviarCodigoVerificacion(@PathVariable Integer id) {
        try {
            sGenerico.enviarCodigoVerificacionCorreo(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Codigo enviado al correo registrado"));
        } catch (Exception e) {
            log.error("Error al enviar codigo de verificacion a clienteId={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Verificar correo con codigo", description = "Valida el codigo de 6 digitos enviado al correo del cliente. Si es correcto y no expiro, marca el correo como verificado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo verificado correctamente"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido o expirado")
    })
    @PostMapping("/{id}/verificar-correo")
    public ResponseEntity<ResponseGeneric<String>> verificarCorreo(
            @PathVariable Integer id, @Valid @RequestBody VerificarCorreoRequest request) {
        try {
            sGenerico.verificarCorreo(id, request.getCodigo());
            return ResponseEntity.ok(new ResponseGeneric<>("Correo verificado correctamente"));
        } catch (Exception e) {
            log.error("Error al verificar correo de clienteId={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Resetear verificacion de correo (solo ADMIN)", description = "Regresa el correo del cliente a 'no verificado' y borra cualquier codigo pendiente. Pensado para pruebas/soporte, no para el flujo normal del cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verificacion reseteada"),
        @ApiResponse(responseCode = "400", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN")
    })
    @DeleteMapping("/{id}/verificacion-correo")
    public ResponseEntity<ResponseGeneric<String>> resetVerificacionCorreo(@PathVariable Integer id) {
        try {
            sGenerico.resetVerificacionCorreo(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Verificacion de correo reseteada"));
        } catch (Exception e) {
            log.error("Error al resetear verificacion de correo de clienteId={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
