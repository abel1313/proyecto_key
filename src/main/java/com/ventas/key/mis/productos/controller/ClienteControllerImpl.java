package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Direccion;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ClienteAdminDetalleDto;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.PreferenciaCorreoRequest;
import com.ventas.key.mis.productos.models.PreferenciaPromocionesRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.SolicitarCambioCorreoRequest;
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

    // save(Cliente, BindingResult) no lleva @Valid -- por eso las anotaciones de Cliente
    // (@Email en correoElectronico, etc.) nunca se disparan aqui, y el front puede mandar
    // cualquier texto como correo (encontrado 2026-09-04: se guardo "qa" como correo de un
    // cliente sin que el back lo rechazara). No se agrega @Valid al parametro completo porque
    // Cliente tiene otros campos @NotBlank/@NotNull (nombrePersona, apeidoPaterno,
    // numeroTelefonico) que este mismo endpoint ya guarda parcialmente en flujos existentes
    // (ej. el Cliente auto-creado al registrarse, que arranca con esos campos vacios a
    // proposito -- ver ClienteServiceImpl.crearClienteDesdeRegistro) -- validar todo el objeto
    // rompería esos casos. Se valida solo el formato del correo, y solo cuando de verdad se
    // esta intentando cambiar (mismo punto donde ya se decide si aplica directo o queda
    // pendiente de verificar, un poco mas abajo).
    // (\.[\w-]+)+ (no solo un ".[a-zA-Z]{2,}" final) -- un dominio de 1 sola terminacion
    // rechazaba en falso cualquier correo con TLD compuesto como "novedades-jade.com.mx"
    // (encontrado 2026-09-04 al probarlo con un correo real del propio dominio del negocio).
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

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
        Integer usuarioIdSolicitado = requestG.getUsuario() != null ? requestG.getUsuario().getId() : null;
        if (!AuthenticationUtils.isAdminContext()
                && (usuarioIdSolicitado == null
                    || !usuarioIdSolicitado.equals(AuthenticationUtils.currentUsuario().getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseGeneric<>(null, "No puedes modificar datos de otro usuario"));
        }

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
            // La preferencia de correos SOLO se cambia via PUT /{id}/preferencias-correo -- si el
            // guardado generico la dejara pasar, cualquier form que no la incluya en su payload
            // (ej. "Mis datos") la resetearia al default de la clase (true) en cada guardado.
            requestG.setRecibirCorreos(existente.getRecibirCorreos());
            // Mismo criterio para el checkbox de promociones -- SOLO se cambia via PUT
            // /{id}/preferencias-promociones.
            requestG.setRecibirPromociones(existente.getRecibirPromociones());

            String correoNuevo = requestG.getCorreoElectronico();
            String correoActual = existente.getCorreoElectronico();
            boolean cambioDeCorreo = correoNuevo != null && !correoNuevo.equalsIgnoreCase(correoActual);
            if (cambioDeCorreo && !EMAIL_PATTERN.matcher(correoNuevo).matches()) {
                throw new RuntimeException("El correo electronico no tiene un formato valido");
            }
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
                // Encontrado 2026-09-04: esta rama nunca sincronizaba hacia Usuario.email (la
                // sincronizacion solo vivia en ClienteServiceImpl.verificarCorreo, el camino con
                // codigo) -- un admin editando su propio correo por aqui dejaba su Cliente
                // actualizado pero su Usuario.email en null para siempre, y cualquier cosa que
                // dependiera de Usuario.email (ej. el aviso de "nuevo pedido" a los admins)
                // nunca lo encontraba.
                if (usr.get().getEmail() == null || !usr.get().getEmail().equalsIgnoreCase(correoNuevo)) {
                    usr.get().setEmail(correoNuevo);
                    usuarioDetailsService.guardar(usr.get());
                }
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

    @Operation(summary = "Actualizar cliente", description = "Alias de guardar: aplica la misma logica de save() (verificacion de correo, direcciones, control de propiedad) en vez del guardado generico crudo.")
    @Override
    public ResponseEntity<ResponseGeneric<Cliente>> update(Integer tipoDato, Cliente requestG, BindingResult result) throws Exception {
        return save(requestG, result);
    }

    @Operation(summary = "Buscar cliente por ID de cliente", description = "Retorna el cliente cuyo ID coincide con el parametro idCliente. Solo el dueno del registro o un ADMIN pueden consultarlo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "403", description = "No es el dueno del registro ni ADMIN"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("buscarPorIdCliente/{idCliente}")
    public ResponseEntity<ResponseGeneric<Cliente>> findByIdCliente(
            @Parameter(description = "ID del cliente") @PathVariable int idCliente) {
        Usuario actual = AuthenticationUtils.currentUsuario();
        boolean esDueno = actual.getCliente() != null && actual.getCliente().getId() != null
                && actual.getCliente().getId() == idCliente;
        if (!AuthenticationUtils.isAdminContext() && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(sGenerico.findClienteById(idCliente));
    }

    @Operation(summary = "Detalle completo de cliente para admin", description = "Igual que buscarPorIdCliente, pero incluye el usuarioId y username del usuario vinculado (Cliente.usuario no viaja en el JSON del endpoint normal) -- lo necesita la pantalla de ver/editar cliente para poder guardar despues.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle encontrado"),
        @ApiResponse(responseCode = "403", description = "No es ADMIN"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("admin/detalle/{idCliente}")
    public ResponseEntity<ResponseGeneric<Optional<ClienteAdminDetalleDto>>> obtenerDetalleAdmin(
            @Parameter(description = "ID del cliente") @PathVariable int idCliente) {
        if (!AuthenticationUtils.isAdminContext()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(sGenerico.obtenerDetalleAdmin(idCliente));
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

    @Operation(summary = "Solicitar cambio de correo del cliente", description = "Manda un codigo de 6 digitos al correo NUEVO (no al actual). El correo real no cambia todavia -- solo se guarda como pendiente hasta confirmar el codigo con /verificar-correo. Pensado para dispararse solo (ej. al salir del campo de correo en 'Mis datos'), sin depender de guardar el resto del formulario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Codigo enviado (o reutilizado uno ya vigente)"),
        @ApiResponse(responseCode = "400", description = "Correo invalido, ya es el correo actual, o cliente no encontrado"),
        @ApiResponse(responseCode = "403", description = "No es el dueno del registro ni ADMIN")
    })
    @PostMapping("/{id}/solicitar-cambio-correo")
    public ResponseEntity<ResponseGeneric<String>> solicitarCambioCorreo(
            @PathVariable Integer id, @Valid @RequestBody SolicitarCambioCorreoRequest request) {
        Usuario actual = AuthenticationUtils.currentUsuario();
        boolean esDueno = actual.getCliente() != null && actual.getCliente().getId() != null
                && actual.getCliente().getId().intValue() == id.intValue();
        if (!AuthenticationUtils.isAdminContext() && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        try {
            boolean enviado = sGenerico.solicitarCambioCorreo(id, request.getCorreoNuevo());
            String mensaje = enviado
                    ? "Codigo enviado al correo nuevo"
                    : "Ya tienes un codigo vigente enviado a ese correo, revisa tu bandeja";
            return ResponseEntity.ok(new ResponseGeneric<>(mensaje));
        } catch (Exception e) {
            log.error("Error al solicitar cambio de correo de clienteId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Enviar codigo de verificacion de correo", description = "Genera un codigo de 6 digitos (expira en 15 minutos) y lo envia al correo registrado del cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Codigo enviado"),
        @ApiResponse(responseCode = "400", description = "Cliente no encontrado o sin correo registrado")
    })
    @PostMapping("/{id}/enviar-codigo-verificacion")
    public ResponseEntity<ResponseGeneric<String>> enviarCodigoVerificacion(@PathVariable Integer id) {
        // Sin este chequeo cualquier usuario autenticado podia mandar el id de OTRO cliente y
        // hacerle llegar codigos de verificacion sin que los pidiera -- no filtra datos (el
        // codigo va al correo YA registrado del dueno), pero es spam/molestia y puede invalidar
        // un codigo que el dueno real estaba a punto de usar (encontrado 2026-08-27 junto con la
        // misma falla en Pedidos). Mismo patron esDueno que findByIdCliente(), arriba.
        Usuario actual = AuthenticationUtils.currentUsuario();
        boolean esDueno = actual.getCliente() != null && actual.getCliente().getId() != null
                && actual.getCliente().getId().intValue() == id.intValue();
        if (!AuthenticationUtils.isAdminContext() && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        try {
            sGenerico.enviarCodigoVerificacionCorreo(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Codigo enviado al correo registrado"));
        } catch (Exception e) {
            log.error("Error al enviar codigo de verificacion a clienteId={}: {}", id, e.getMessage(), e);
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
            log.error("Error al verificar correo de clienteId={}: {}", id, e.getMessage(), e);
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
            log.error("Error al resetear verificacion de correo de clienteId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Activar/desactivar correos no transaccionales", description = "Correo de seguimiento de pedido y alerta de stock de favoritos. No afecta el ticket de compra ni los codigos de verificacion/reset, que siguen enviandose siempre. Solo el dueno del registro o un ADMIN pueden cambiarla.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencia actualizada"),
        @ApiResponse(responseCode = "403", description = "No es el dueno del registro ni ADMIN"),
        @ApiResponse(responseCode = "400", description = "Cliente no encontrado")
    })
    @PutMapping("/{id}/preferencias-correo")
    public ResponseEntity<ResponseGeneric<String>> actualizarPreferenciaCorreo(
            @PathVariable Integer id, @RequestBody PreferenciaCorreoRequest request) {
        Usuario actual = AuthenticationUtils.currentUsuario();
        boolean esDueno = actual.getCliente() != null && actual.getCliente().getId() != null
                && actual.getCliente().getId().intValue() == id.intValue();
        if (!AuthenticationUtils.isAdminContext() && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        try {
            return ResponseEntity.ok(sGenerico.actualizarPreferenciaCorreo(id, request.isRecibirCorreos()));
        } catch (Exception e) {
            // Pasar la excepcion completa (no solo e.getMessage()) -- si no, SLF4J nunca imprime
            // el stacktrace ni la cadena de "Caused by", y un mensaje generico como "Could not
            // commit JPA transaction" (2026-09-04, clienteId=23) se queda sin forma de saber la
            // causa real.
            log.error("Error al actualizar preferencia de correo de clienteId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @Operation(summary = "Activar/desactivar correos de promociones", description = "Checkbox independiente del de preferencias-correo -- controla solo si el cliente recibe el correo cuando el admin envia una promocion nueva. Solo el dueno del registro o un ADMIN pueden cambiarla.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencia actualizada"),
        @ApiResponse(responseCode = "403", description = "No es el dueno del registro ni ADMIN"),
        @ApiResponse(responseCode = "400", description = "Cliente no encontrado")
    })
    @PutMapping("/{id}/preferencias-promociones")
    public ResponseEntity<ResponseGeneric<String>> actualizarPreferenciaPromociones(
            @PathVariable Integer id, @RequestBody PreferenciaPromocionesRequest request) {
        Usuario actual = AuthenticationUtils.currentUsuario();
        boolean esDueno = actual.getCliente() != null && actual.getCliente().getId() != null
                && actual.getCliente().getId().intValue() == id.intValue();
        if (!AuthenticationUtils.isAdminContext() && !esDueno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
        }
        try {
            return ResponseEntity.ok(sGenerico.actualizarPreferenciaPromociones(id, request.isRecibirPromociones()));
        } catch (Exception e) {
            log.error("Error al actualizar preferencia de promociones de clienteId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
