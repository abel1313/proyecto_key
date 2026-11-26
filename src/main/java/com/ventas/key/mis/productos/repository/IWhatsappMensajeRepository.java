package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.WhatsappMensajeEntidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface IWhatsappMensajeRepository extends JpaRepository<WhatsappMensajeEntidad, Long> {

    boolean existsByWaMessageId(String waMessageId);

    Optional<WhatsappMensajeEntidad> findByWaMessageId(String waMessageId);

    // Ordena por id, NO por fecha: la columna es DATETIME (segundos) y varios mensajes del mismo
    // segundo empatan. El id autoincremental es el orden real de llegada (mismo cuidado que chat_mensaje).
    Page<WhatsappMensajeEntidad> findByClienteIdOrderByIdDesc(Long clienteId, Pageable pageable);

    boolean existsByClienteIdAndDireccionAndEstadoIn(Long clienteId, String direccion, Collection<String> estados);

    @Query("""
            select count(m) from WhatsappMensajeEntidad m
            where m.clienteId = :clienteId and m.direccion = 'ENTRANTE'
              and m.id > coalesce((select max(s.id) from WhatsappMensajeEntidad s
                                   where s.clienteId = :clienteId and s.direccion = 'SALIENTE'
                                     and s.estado in ('ENVIADO', 'ENTREGADO', 'LEIDO')), 0)
            """)
    long contarEntrantesSinResponder(@Param("clienteId") Long clienteId);
}
