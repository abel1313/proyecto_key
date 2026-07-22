package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository;
import com.ventas.key.mis.productos.service.api.IClienteSinRegistro;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ClienteSinRegistroImpl extends CrudAbstractServiceImpl<ClienteSinRegistro, List<ClienteSinRegistro>, Optional<ClienteSinRegistro>, Integer, PginaDto<List<ClienteSinRegistro>>>
        implements IClienteSinRegistro {

    private static final int CODIGO_EXPIRA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IClienteSinRegistroRepository iClienteSinRegistroRepository;
    private final EmailService emailService;

    public ClienteSinRegistroImpl(
            final IClienteSinRegistroRepository iRepository,
            final ErrorGenerico eGenerico,
            final EmailService emailService
    ){
        super(iRepository, eGenerico);
        this.iClienteSinRegistroRepository = iRepository;
        this.emailService = emailService;
    }

    // Crea el registro ANTES de generar la venta, para poder verificar el correo en ese momento
    // (el flujo anterior lo creaba de un jalon dentro de POST /v1/ventas/save, sin oportunidad
    // de verificar nada antes de guardar el pedido).
    public ClienteSinRegistro crear(ClienteSinRegistroDto dto) {
        ClienteSinRegistro c = new ClienteSinRegistro();
        c.setNombrePersona(dto.getNombre_persona());
        c.setSegundoNombre(dto.getSegundo_nombre());
        c.setApeidoPaterno(dto.getApeido_Paterno());
        c.setApeidoMaterno(dto.getApeido_Materno());
        c.setSexo(dto.getSexo());
        c.setCorreoElectronico(dto.getCorreo_Electronico());
        String fecha = dto.getFecha_Nacimiento();
        c.setFechaNacimiento(fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha));
        c.setNumeroTelefonico(dto.getNumero_Telefonico());
        c.setCorreoVerificado(false);
        return iClienteSinRegistroRepository.save(c);
    }

    public void enviarCodigoVerificacion(Integer id) {
        ClienteSinRegistro c = iClienteSinRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente sin registro no encontrado"));
        if (c.getCorreoElectronico() == null || c.getCorreoElectronico().isBlank()) {
            throw new RuntimeException("El cliente no tiene correo registrado");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        c.setCodigoVerificacion(codigo);
        c.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRA_MINUTOS));
        iClienteSinRegistroRepository.save(c);
        emailService.enviarCodigoVerificacion(c.getCorreoElectronico(), codigo);
    }

    public void verificarCodigo(Integer id, String codigo) {
        ClienteSinRegistro c = iClienteSinRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente sin registro no encontrado"));
        if (Boolean.TRUE.equals(c.getCorreoVerificado())) {
            return;
        }
        if (c.getCodigoVerificacion() == null || !c.getCodigoVerificacion().equals(codigo)) {
            throw new RuntimeException("Codigo de verificacion invalido");
        }
        if (c.getCodigoVerificacionExpira() == null
                || LocalDateTime.now().isAfter(c.getCodigoVerificacionExpira())) {
            throw new RuntimeException("El codigo de verificacion expiro, solicita uno nuevo");
        }
        c.setCorreoVerificado(true);
        c.setCodigoVerificacion(null);
        c.setCodigoVerificacionExpira(null);
        iClienteSinRegistroRepository.save(c);
    }
}
