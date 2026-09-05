package com.ventas.key.mis.productos.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ClienteAdminDetalleDto;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.IClienteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;

@Service
public class ClienteServiceImpl extends CrudAbstractServiceImpl<Cliente, List<Cliente>, Optional<Cliente>, Integer, PginaDto<List<Cliente>>>
implements IClienteService {

    private static final int CODIGO_EXPIRA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IClienteRepository iClienteRepository;
    private final IUsuarioRepository iUsuarioRepository;
    private final ErrorGenerico errorGenerico;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    public ClienteServiceImpl(
        final IClienteRepository iRepository,
        final IUsuarioRepository iUsuarioRepository,
        final ErrorGenerico eGenerico,
        final EmailService emailService
    ){
        super(iRepository, eGenerico);
        this.iClienteRepository = iRepository;
        this.iUsuarioRepository = iUsuarioRepository;
        this.errorGenerico = eGenerico;
        this.emailService = emailService;
    }

    /**
     * Auto-alta del Cliente al verificar el correo de un Usuario recien registrado (mejora 15).
     * Bypass deliberado de Bean Validation (nombre/apellidos/telefono aun no existen) via INSERT
     * nativo — un repository.save() normal dispararia @NotBlank/@NotNull de Cliente.java.
     *
     * nombre_persona y apeido_paterno van como '' (no NULL): a diferencia de
     * correo_electronico/numero_telefonico/apeido_materno, esas dos columnas siguen siendo
     * NOT NULL sin default en la BD real -- el INSERT sin ellas tronaba con
     * "Field 'nombre_persona' doesn't have a default value" al verificar el correo de una
     * cuenta nueva (QA 2026-09-02). '' se trata igual que NULL en toda la app (ver
     * Cliente.recalcularDatosCompletos(), que usa isBlank()), asi que no cambia el significado
     * de "datos incompletos" -- solo evita la excepcion sin tener que alterar la tabla.
     */
    @Transactional
    public Cliente crearClienteDesdeRegistro(Usuario usuario, String correo) {
        entityManager.createNativeQuery(
                "INSERT INTO clientes (usuario_id, correo_electronico, correo_verificado, datos_completos, nombre_persona, apeido_paterno) " +
                "VALUES (:usuarioId, :correo, 1, 0, '', '')")
                .setParameter("usuarioId", usuario.getId())
                .setParameter("correo", correo)
                .executeUpdate();
        return iClienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Error al auto-crear cliente para usuario " + usuario.getId()));
    }

    /** Toggle de correos no transaccionales (seguimiento de pedido, alerta de stock). */
    @Transactional
    public ResponseGeneric<String> actualizarPreferenciaCorreo(int idCliente, boolean recibirCorreos) {
        Cliente cliente = iClienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setRecibirCorreos(recibirCorreos);
        iClienteRepository.save(cliente);
        return new ResponseGeneric<>(recibirCorreos
                ? "Notificaciones por correo activadas"
                : "Notificaciones por correo desactivadas");
    }

    /** Toggle independiente del de arriba -- controla solo los correos de promociones. */
    @Transactional
    public ResponseGeneric<String> actualizarPreferenciaPromociones(int idCliente, boolean recibirPromociones) {
        Cliente cliente = iClienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setRecibirPromociones(recibirPromociones);
        iClienteRepository.save(cliente);
        return new ResponseGeneric<>(recibirPromociones
                ? "Correos de promociones activados"
                : "Correos de promociones desactivados");
    }

    // Mismo patron que UsuarioVerificacionService.solicitarCambioCorreo, pero para Cliente --
    // pedido 2026-09-04: cambiar el correo desde "Mis datos" cuando ya esta verificado debia
    // poder dispararse solo (al salir del campo), sin depender de guardar el resto del
    // formulario primero como hacia el flujo viejo (embebido en ClienteControllerImpl.save()).
    // Duplica el regex de EMAIL_PATTERN (ClienteControllerImpl) a proposito: son dos puntos de
    // entrada distintos para el mismo cambio de correo (el guardado general del formulario, y
    // este endpoint dedicado), cada uno necesita validar el formato en su propio punto.
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /**
     * Solicita el cambio de correo del propio cliente (o el que edita un admin) -- verificar
     * antes de guardar: el correo real NO se toca aqui, solo se guarda como correoPendiente y se
     * manda el codigo a la direccion nueva. Si el codigo nunca se confirma (verificarCorreo), el
     * correo real nunca cambia.
     *
     * Si ya hay un codigo vigente para el MISMO correo nuevo (no expiro), no se reenvia correo --
     * evita que reintentos/doble-click (o un doble blur del input) manden varios correos con
     * codigos distintos. Devuelve true si mando un correo nuevo, false si reutilizo uno vigente.
     */
    @Transactional
    public boolean solicitarCambioCorreo(Integer clienteId, String correoNuevo) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (correoNuevo == null || correoNuevo.isBlank()) {
            throw new RuntimeException("El correo nuevo es requerido");
        }
        if (!EMAIL_PATTERN.matcher(correoNuevo).matches()) {
            throw new RuntimeException("El correo electronico no tiene un formato valido");
        }
        if (correoNuevo.equalsIgnoreCase(cliente.getCorreoElectronico())) {
            throw new RuntimeException("Ese ya es el correo actual");
        }
        // Sin este chequeo, dos clientes podian terminar con el mismo correo -- el duplicado
        // recien se detectaba al confirmar, con un fallo de commit de JPA (hotfix 2026-09-05,
        // mismo patron que UsuarioVerificacionService.solicitarCambioCorreo).
        iClienteRepository.findFirstByCorreoElectronicoIgnoreCase(correoNuevo).ifPresent(otro -> {
            if (!otro.getId().equals(cliente.getId())) {
                throw new ExceptionDuplicado("Ese correo ya está en uso por otra cuenta");
            }
        });
        boolean yaVigente = correoNuevo.equalsIgnoreCase(cliente.getCorreoPendiente())
                && cliente.getCodigoVerificacionExpira() != null
                && LocalDateTime.now().isBefore(cliente.getCodigoVerificacionExpira());
        if (yaVigente) {
            return false;
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        cliente.setCorreoPendiente(correoNuevo);
        cliente.setCodigoVerificacion(codigo);
        cliente.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        iClienteRepository.save(cliente);
        boolean correoEnviado = emailService.enviarCodigoVerificacion(correoNuevo, codigo);
        if (!correoEnviado) {
            throw new RuntimeException("No se pudo enviar el correo de verificacion, intenta de nuevo en unos minutos");
        }
        return true;
    }

    public void enviarCodigoVerificacionCorreo(Integer clienteId) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        // Si hay un correo pendiente de verificar (mejora 15, cambio de correo), el codigo va a
        // ese correo nuevo, no al ya verificado.
        String destino = cliente.getCorreoPendiente() != null && !cliente.getCorreoPendiente().isBlank()
                ? cliente.getCorreoPendiente() : cliente.getCorreoElectronico();
        if (destino == null || destino.isBlank()) {
            throw new RuntimeException("El cliente no tiene correo registrado");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        cliente.setCodigoVerificacion(codigo);
        cliente.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        iClienteRepository.save(cliente);
        emailService.enviarCodigoVerificacion(destino, codigo);
    }

    public void verificarCorreo(Integer clienteId, String codigo) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        boolean hayCorreoPendiente = cliente.getCorreoPendiente() != null && !cliente.getCorreoPendiente().isBlank();
        if (Boolean.TRUE.equals(cliente.getCorreoVerificado()) && !hayCorreoPendiente) {
            return;
        }
        if (cliente.getCodigoVerificacion() == null || !cliente.getCodigoVerificacion().equals(codigo)) {
            throw new RuntimeException("Codigo de verificacion invalido");
        }
        if (cliente.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(cliente.getCodigoVerificacionExpira())) {
            throw new RuntimeException("El codigo de verificacion expiro, solicita uno nuevo");
        }
        // Mismo chequeo que solicitarCambioCorreo, repetido aqui porque el correo pendiente ya
        // pudo quedar en conflicto ANTES de ese fix, o alguien mas tomo ese correo mientras el
        // codigo seguia vigente -- sin esto, confirmar revienta el commit igual (hotfix
        // 2026-09-05). Se limpia el pendiente para no dejar la cuenta atascada.
        if (hayCorreoPendiente) {
            iClienteRepository.findFirstByCorreoElectronicoIgnoreCase(cliente.getCorreoPendiente())
                    .filter(otro -> !otro.getId().equals(cliente.getId()))
                    .ifPresent(otro -> {
                        cliente.setCorreoPendiente(null);
                        cliente.setCodigoVerificacion(null);
                        cliente.setCodigoVerificacionExpira(null);
                        iClienteRepository.save(cliente);
                        throw new ExceptionDuplicado(
                                "Ese correo ya está en uso por otra cuenta, solicita el cambio con uno distinto");
                    });
        }
        // Mejora 15: si habia un correo nuevo pendiente, se promueve ahora y se sincroniza con
        // Usuario.email — hasta este momento correoElectronico seguia siendo el anterior.
        if (hayCorreoPendiente) {
            cliente.setCorreoElectronico(cliente.getCorreoPendiente());
            cliente.setCorreoPendiente(null);
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setEmail(cliente.getCorreoElectronico());
                iUsuarioRepository.save(cliente.getUsuario());
            }
        }
        cliente.setCorreoVerificado(true);
        cliente.setCodigoVerificacion(null);
        cliente.setCodigoVerificacionExpira(null);
        iClienteRepository.save(cliente);
    }

    /** Solo para pruebas/soporte — regresa el correo del cliente a "no verificado". */
    public void resetVerificacionCorreo(Integer clienteId) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setCorreoVerificado(false);
        cliente.setCodigoVerificacion(null);
        cliente.setCodigoVerificacionExpira(null);
        iClienteRepository.save(cliente);
    }

    /**
     * Devuelve Cliente (no Optional) a proposito: cachear un ResponseGeneric<Optional<Cliente>>
     * en Redis rompia la lectura de vuelta -- el serializador de Redis guarda el Optional con
     * metadatos de tipo ("@class") que jackson-datatype-jdk8 no sabe reconstruir, y la siguiente
     * peticion (cache hit) tronaba con "Cannot construct instance of java.util.Optional" en vez
     * de devolver el cliente. Reproducido en QA 2026-09-02 al generar un pedido: el cliente
     * recien registrado no se podia leer por este endpoint una vez que la respuesta quedaba en
     * cache. El front (cliente.service.ts) ya esperaba un Cliente plano en `data`, no un
     * Optional, asi que este cambio no le afecta.
     *
     * Mismo problema, otra variante (encontrado 2026-09-03): listDirecciones es @OneToMany LAZY,
     * asi que Hibernate lo entrega como un proxy (org.hibernate.collection.internal.PersistentSet),
     * no un Set comun. El ObjectMapper de Redis usa "default typing" (CacheTtlConfig/RedisConfig)
     * para poder reconstruir el tipo real al leer -- eso significa que guarda el nombre de esa
     * clase de Hibernate en el JSON. En la siguiente peticion (cache hit), Jackson intenta
     * reconstruir un PersistentSet de verdad, que exige una Session de Hibernate activa -- y como
     * ya es una peticion distinta, no hay ninguna, y truena con "failed to lazily initialize a
     * collection: could not initialize proxy - no Session" (se ve como "Could not read JSON"
     * porque asi reporta el error GenericJackson2JsonRedisSerializer.deserialize()). Se fuerza la
     * carga aqui (todavia con la Session de esta peticion abierta) y se copia a un LinkedHashSet
     * comun antes de cachear, para que lo que se guarde en Redis sea un tipo que Jackson pueda
     * reconstruir sin depender de Hibernate.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "clienteCache", key = "#id")
    public ResponseGeneric<Cliente> findClienteById(int id) {
        Cliente cliente = this.iClienteRepository.findClienteById(id).orElse(null);
        if (cliente != null && cliente.getListDirecciones() != null) {
            cliente.setListDirecciones(new LinkedHashSet<>(cliente.getListDirecciones()));
        }
        return new ResponseGeneric<>(cliente);
    }

    @Override
    public ResponseGeneric<Optional<ClienteAdminDetalleDto>> obtenerDetalleAdmin(int id) {
        Optional<Cliente> clienteOpt = this.iClienteRepository.findClienteById(id);
        if (clienteOpt.isEmpty()) {
            return new ResponseGeneric<>(Optional.empty());
        }
        Cliente cliente = clienteOpt.get();
        Usuario usuario = cliente.getUsuario();
        ClienteAdminDetalleDto dto = new ClienteAdminDetalleDto(
                cliente,
                usuario != null ? usuario.getId() : null,
                usuario != null ? usuario.getUsername() : null
        );
        return new ResponseGeneric<>(Optional.of(dto));
    }

    @Override
    @Cacheable(value = "clienteCache", key = "#nombre + ':' + #page + ':' + #size")
    public PageableDto<List<ClienteBusquedaDto>> buscarClientes(String nombre, int page, int size) {
        Page<ClienteBusquedaDto> resultado = iClienteRepository.buscarPorNombre(nombre, PageRequest.of(page, size));
        PageableDto<List<ClienteBusquedaDto>> dto = new PageableDto<>();
        dto.setList(resultado.getContent());
        dto.setTotalPaginas(resultado.getTotalPages());
        return dto;
    }
}
