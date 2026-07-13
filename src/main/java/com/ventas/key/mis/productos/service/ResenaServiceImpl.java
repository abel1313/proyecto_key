package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Resena;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.resenas.ResenaEditarDto;
import com.ventas.key.mis.productos.models.resenas.ResenaRequestDto;
import com.ventas.key.mis.productos.models.resenas.ResenaResponseDto;
import com.ventas.key.mis.productos.models.resenas.ResenaResumenDto;
import com.ventas.key.mis.productos.repository.IDetalleVentaVarianteRepository;
import com.ventas.key.mis.productos.repository.IResenaRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResenaServiceImpl {

    private final IResenaRepository iResenaRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IDetalleVentaVarianteRepository iDetalleVentaVarianteRepository;

    private Cliente clienteActual() {
        Usuario usuario = AuthenticationUtils.currentUsuario();
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            throw new RuntimeException("Tu cuenta todavia no tiene un perfil de cliente completo");
        }
        return cliente;
    }

    @Transactional
    public ResenaResponseDto crear(ResenaRequestDto dto) {
        if (dto.getCalificacion() == null || dto.getCalificacion() < 1 || dto.getCalificacion() > 5) {
            throw new RuntimeException("La calificacion debe ser un numero entre 1 y 5");
        }
        Cliente cliente = clienteActual();
        Variantes variante = iVarianteRepository.findById(dto.getVarianteId())
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id: " + dto.getVarianteId()));

        if (iResenaRepository.existsByCliente_IdAndVariante_Id(cliente.getId(), variante.getId())) {
            throw new RuntimeException("Ya dejaste una resena para este producto, puedes editarla en vez de crear otra");
        }
        if (!iDetalleVentaVarianteRepository.existsByVariante_IdAndVenta_Cliente_Id(variante.getId(), cliente.getId())) {
            throw new RuntimeException("Solo puedes resenar productos que hayas comprado");
        }

        Resena resena = new Resena();
        resena.setCliente(cliente);
        resena.setVariante(variante);
        resena.setCalificacion(dto.getCalificacion());
        resena.setComentario(dto.getComentario());
        resena.setFechaCreacion(LocalDateTime.now());
        Resena guardada = iResenaRepository.save(resena);
        return toResponseDto(guardada, cliente.getId());
    }

    @Transactional
    public ResenaResponseDto editar(Integer resenaId, ResenaEditarDto dto) {
        if (dto.getCalificacion() == null || dto.getCalificacion() < 1 || dto.getCalificacion() > 5) {
            throw new RuntimeException("La calificacion debe ser un numero entre 1 y 5");
        }
        Cliente cliente = clienteActual();
        Resena resena = iResenaRepository.findById(resenaId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la resena con id: " + resenaId));
        if (!resena.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("No puedes editar la resena de otro cliente");
        }
        resena.setCalificacion(dto.getCalificacion());
        resena.setComentario(dto.getComentario());
        Resena guardada = iResenaRepository.save(resena);
        return toResponseDto(guardada, cliente.getId());
    }

    // Dueno borra la suya; ADMIN borra cualquiera (moderacion de contenido inapropiado). No hay
    // cola de aprobacion previa -- la resena se publica de inmediato al crearse y se modera
    // despues, borrandola si hace falta.
    @Transactional
    public void eliminar(Integer resenaId) {
        Resena resena = iResenaRepository.findById(resenaId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la resena con id: " + resenaId));
        boolean esAdmin = AuthenticationUtils.isAdminContext();
        if (!esAdmin) {
            Cliente cliente = clienteActual();
            if (!resena.getCliente().getId().equals(cliente.getId())) {
                throw new RuntimeException("No puedes eliminar la resena de otro cliente");
            }
        }
        iResenaRepository.deleteById(resenaId);
    }

    public PginaDto<List<ResenaResponseDto>> listarPorVariante(Integer varianteId, int pagina, int size) {
        Page<Resena> page = iResenaRepository.findByVariante_IdOrderByFechaCreacionDesc(varianteId, PageRequest.of(pagina - 1, size));
        Integer clienteId = AuthenticationUtils.currentUsuarioOpt()
                .map(Usuario::getCliente)
                .map(Cliente::getId)
                .orElse(null);

        PginaDto<List<ResenaResponseDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(r -> toResponseDto(r, clienteId)).toList());
        return resultado;
    }

    public PginaDto<List<ResenaResponseDto>> misResenas(int pagina, int size) {
        Cliente cliente = clienteActual();
        Page<Resena> page = iResenaRepository.findByCliente_IdOrderByFechaCreacionDesc(cliente.getId(), PageRequest.of(pagina - 1, size));

        PginaDto<List<ResenaResponseDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(r -> toResponseDto(r, cliente.getId())).toList());
        return resultado;
    }

    public ResenaResumenDto resumenPorVariante(Integer varianteId) {
        Object[] fila = iResenaRepository.resumenPorVariante(varianteId);
        Double promedio = fila[0] != null ? ((Number) fila[0]).doubleValue() : 0.0;
        Long total = fila[1] != null ? ((Number) fila[1]).longValue() : 0L;

        Map<Integer, Long> conteoPorEstrella = new HashMap<>();
        for (int estrella = 1; estrella <= 5; estrella++) conteoPorEstrella.put(estrella, 0L);
        for (Object[] filaConteo : iResenaRepository.conteoPorEstrella(varianteId)) {
            conteoPorEstrella.put(((Number) filaConteo[0]).intValue(), ((Number) filaConteo[1]).longValue());
        }

        return new ResenaResumenDto(varianteId, Math.round(promedio * 10) / 10.0, total, conteoPorEstrella);
    }

    private ResenaResponseDto toResponseDto(Resena resena, Integer clienteActualId) {
        Cliente c = resena.getCliente();
        String inicialApellido = (c.getApeidoPaterno() != null && !c.getApeidoPaterno().isBlank())
                ? c.getApeidoPaterno().substring(0, 1).toUpperCase() + "."
                : "";
        String nombreCliente = (c.getNombrePersona() + " " + inicialApellido).trim();

        return new ResenaResponseDto(
                resena.getId(),
                resena.getVariante().getId(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getFechaCreacion(),
                nombreCliente,
                clienteActualId != null && clienteActualId.equals(c.getId()));
    }
}
