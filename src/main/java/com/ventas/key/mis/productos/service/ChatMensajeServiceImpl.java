package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.repository.IChatMensajeRepository;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public Optional<ChatMensaje> ultimoMensaje(String sesionId) {
        return repository.findTop1BySesionIdOrderByTimestampDesc(sesionId);
    }
}
