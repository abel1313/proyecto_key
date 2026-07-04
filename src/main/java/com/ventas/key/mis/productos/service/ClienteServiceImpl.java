package com.ventas.key.mis.productos.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.entity.Usuario;
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
     */
    @Transactional
    public Cliente crearClienteDesdeRegistro(Usuario usuario, String correo) {
        entityManager.createNativeQuery(
                "INSERT INTO clientes (usuario_id, correo_electronico, correo_verificado, datos_completos) " +
                "VALUES (:usuarioId, :correo, 1, 0)")
                .setParameter("usuarioId", usuario.getId())
                .setParameter("correo", correo)
                .executeUpdate();
        return iClienteRepository.findClienteById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Error al auto-crear cliente para usuario " + usuario.getId()));
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

    @Override
    @Cacheable(value = "clienteCache", key = "#id")
    public ResponseGeneric<Optional<Cliente>> findClienteById(int id) {
        return new ResponseGeneric<>(this.iClienteRepository.findClienteById(id));
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
