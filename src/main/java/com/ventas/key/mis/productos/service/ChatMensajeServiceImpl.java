package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.models.chat.ChatHistorialPaginadoDto;
import com.ventas.key.mis.productos.repository.IChatMensajeRepository;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMensajeServiceImpl implements IChatMensajeService {

    private final IChatMensajeRepository repository;

    public ChatMensajeServiceImpl(IChatMensajeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ChatMensaje guardar(String sesionId, String remitente, String contenido) {
        return repository.save(ChatMensaje.builder()
                .sesionId(sesionId)
                .remitente(remitente)
                .contenido(contenido)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    public List<ChatMensaje> obtenerHistorial(String sesionId) {
        return repository.findBySesionIdOrderByTimestampAsc(sesionId);
    }

    @Override
    public ChatHistorialPaginadoDto obtenerHistorialPaginado(String sesionId, int pagina, int size) {
        Page<ChatMensaje> page = repository.findBySesionIdOrderByTimestampDesc(
            sesionId, PageRequest.of(pagina, size)
        );
        // El repo devuelve DESC (más reciente primero) — invertir para mostrar cronológico en el front
        List<ChatMensaje> mensajes = new ArrayList<>(page.getContent());
        java.util.Collections.reverse(mensajes);
        return ChatHistorialPaginadoDto.builder()
                .mensajes(mensajes)
                .pagina(pagina)
                .totalPaginas(page.getTotalPages())
                .totalMensajes(page.getTotalElements())
                .hayMasAntiguos(page.hasNext())
                .build();
    }

    @Override
    public ChatHistorialPaginadoDto obtenerHistorialPorClienteId(String clienteId, int pagina, int size) {
        Page<ChatMensaje> page = repository.findByClienteIdOrderByTimestampDesc(
            clienteId, PageRequest.of(pagina, size)
        );
        List<ChatMensaje> mensajes = new ArrayList<>(page.getContent());
        java.util.Collections.reverse(mensajes);
        return ChatHistorialPaginadoDto.builder()
                .mensajes(mensajes)
                .pagina(pagina)
                .totalPaginas(page.getTotalPages())
                .totalMensajes(page.getTotalElements())
                .hayMasAntiguos(page.hasNext())
                .build();
    }

    @Override
    public Optional<ChatMensaje> ultimoMensaje(String sesionId) {
        return repository.findTop1BySesionIdOrderByTimestampDesc(sesionId);
    }
}
