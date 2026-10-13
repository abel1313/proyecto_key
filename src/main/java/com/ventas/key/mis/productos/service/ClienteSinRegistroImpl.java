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
        validar(dto);
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

    /**
     * Lo que se escribe en Venta directa (QA 2026-10-08: se guardaban clientes con nombre "a").
     * Nombre obligatorio con al menos 3 letras; los opcionales, si se escriben, tambien 3 letras;
     * el correo con formato de correo y el telefono con 10 digitos.
     */
    static void validar(ClienteSinRegistroDto dto) {
        if (letras(dto.getNombre_persona()) < MINIMO_LETRAS) {
            throw new RuntimeException("El nombre debe tener al menos " + MINIMO_LETRAS + " letras");
        }
        validarOpcional(dto.getSegundo_nombre(), "El segundo nombre");
        validarOpcional(dto.getApeido_Paterno(), "El apellido paterno");
        validarOpcional(dto.getApeido_Materno(), "El apellido materno");
        String correo = dto.getCorreo_Electronico();
        if (correo != null && !correo.isBlank() && !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("El correo no es valido");
        }
        String telefono = dto.getNumero_Telefonico();
        if (telefono != null && !telefono.isBlank()) {
            String digitos = telefono.replaceAll("[\\s()+-]", "");
            if (digitos.length() == 12 && digitos.startsWith("52")) digitos = digitos.substring(2);
            if (!digitos.matches("\\d{10}")) {
                throw new RuntimeException("El telefono debe tener 10 digitos");
            }
        }
    }

    private static final int MINIMO_LETRAS = 3;

    private static void validarOpcional(String valor, String campo) {
        if (valor != null && !valor.isBlank() && letras(valor) < MINIMO_LETRAS) {
            throw new RuntimeException(campo + " debe tener al menos " + MINIMO_LETRAS + " letras (o dejalo vacio)");
        }
    }

    private static long letras(String valor) {
        return valor == null ? 0 : valor.codePoints().filter(Character::isLetter).count();
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
