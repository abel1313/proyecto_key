package com.ventas.key.mis.productos.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.IClienteService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;

@Service
public class ClienteServiceImpl extends CrudAbstractServiceImpl<Cliente, List<Cliente>, Optional<Cliente>, Integer, PginaDto<List<Cliente>>>
implements IClienteService {

    private static final int CODIGO_EXPIRA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IClienteRepository iClienteRepository;
    private final ErrorGenerico errorGenerico;
    private final EmailService emailService;

    public ClienteServiceImpl(
        final IClienteRepository iRepository,
        final ErrorGenerico eGenerico,
        final EmailService emailService
    ){
        super(iRepository, eGenerico);
        this.iClienteRepository = iRepository;
        this.errorGenerico = eGenerico;
        this.emailService = emailService;
    }

    public void enviarCodigoVerificacionCorreo(Integer clienteId) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (cliente.getCorreoElectronico() == null || cliente.getCorreoElectronico().isBlank()) {
            throw new RuntimeException("El cliente no tiene correo registrado");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        cliente.setCodigoVerificacion(codigo);
        cliente.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        iClienteRepository.save(cliente);
        emailService.enviarCodigoVerificacion(cliente.getCorreoElectronico(), codigo);
    }

    public void verificarCorreo(Integer clienteId, String codigo) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (Boolean.TRUE.equals(cliente.getCorreoVerificado())) {
            return;
        }
        if (cliente.getCodigoVerificacion() == null || !cliente.getCodigoVerificacion().equals(codigo)) {
            throw new RuntimeException("Codigo de verificacion invalido");
        }
        if (cliente.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(cliente.getCodigoVerificacionExpira())) {
            throw new RuntimeException("El codigo de verificacion expiro, solicita uno nuevo");
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
