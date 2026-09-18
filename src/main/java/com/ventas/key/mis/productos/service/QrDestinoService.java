package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.qr.QrDestinoCreateDto;
import com.ventas.key.mis.productos.dto.qr.QrDestinoPublicoDto;
import com.ventas.key.mis.productos.dto.qr.QrDestinoUpdateDto;
import com.ventas.key.mis.productos.entity.QrDestino;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.repository.IQrDestinoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrDestinoService {

    private static final int URL_MAX = 1000;

    private final IQrDestinoRepository qrDestinoRepo;

    public List<QrDestinoPublicoDto> listarPublico() {
        return qrDestinoRepo.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .map(d -> new QrDestinoPublicoDto(d.getId(), d.getNombre(), d.getUrl(),
                        d.getDescripcion(), d.getIcono()))
                .toList();
    }

    public List<QrDestino> listarTodos() {
        return qrDestinoRepo.findAllByOrderByOrdenAscIdAsc();
    }

    @Transactional
    public QrDestino crear(QrDestinoCreateDto dto) {
        QrDestino destino = new QrDestino();
        destino.setNombre(exigirNombre(dto.getNombre()));
        destino.setUrl(exigirUrl(dto.getUrl()));
        destino.setDescripcion(limpiar(dto.getDescripcion()));
        destino.setIcono(limpiar(dto.getIcono()));
        destino.setActivo(true);
        destino.setOrden(dto.getOrden() != null ? dto.getOrden() : siguienteOrden());
        return qrDestinoRepo.save(destino);
    }

    @Transactional
    public QrDestino actualizar(Integer id, QrDestinoUpdateDto dto) {
        QrDestino destino = qrDestinoRepo.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("No se encontró el destino de QR con id " + id));
        if (dto.getNombre() != null) destino.setNombre(exigirNombre(dto.getNombre()));
        if (dto.getUrl() != null) destino.setUrl(exigirUrl(dto.getUrl()));
        if (dto.getDescripcion() != null) destino.setDescripcion(limpiar(dto.getDescripcion()));
        if (dto.getIcono() != null) destino.setIcono(limpiar(dto.getIcono()));
        if (dto.getActivo() != null) destino.setActivo(dto.getActivo());
        if (dto.getOrden() != null) destino.setOrden(dto.getOrden());
        return qrDestinoRepo.save(destino);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!qrDestinoRepo.existsById(id)) {
            throw new ExceptionDataNotFound("No se encontró el destino de QR con id " + id);
        }
        qrDestinoRepo.deleteById(id);
    }

    private int siguienteOrden() {
        return qrDestinoRepo.findAllByOrderByOrdenAscIdAsc().stream()
                .map(QrDestino::getOrden)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private String exigirNombre(String nombre) {
        String limpio = limpiar(nombre);
        if (limpio == null) {
            throw new ExceptionOperacionNoPermitida("El nombre del destino es obligatorio");
        }
        return limpio;
    }

    /**
     * Solo se aceptan http y https. Un QR se escanea con la cámara del teléfono y se abre sin que
     * nadie lea la URL antes, así que esquemas como javascript:, data: o intent: no pueden entrar
     * aquí ni aunque quien los dé de alta sea admin.
     */
    private String exigirUrl(String url) {
        String limpio = limpiar(url);
        if (limpio == null) {
            throw new ExceptionOperacionNoPermitida("La URL del destino es obligatoria");
        }
        if (limpio.length() > URL_MAX) {
            throw new ExceptionOperacionNoPermitida("La URL no puede pasar de " + URL_MAX + " caracteres");
        }
        String esquema;
        try {
            esquema = URI.create(limpio).getScheme();
        } catch (IllegalArgumentException e) {
            throw new ExceptionOperacionNoPermitida("La URL no tiene un formato válido: " + limpio);
        }
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
            throw new ExceptionOperacionNoPermitida(
                    "La URL debe empezar con http:// o https:// — se recibió: " + limpio);
        }
        return limpio;
    }

    private String limpiar(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
