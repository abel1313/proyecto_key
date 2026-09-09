package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Favorito;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IFavoritoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aviso de "volvio el stock" a quienes tienen una variante en Favoritos -- extraido de
 * VarianteServiceImpl.notificarRestock (2026-09-04) para que tambien lo disparen los lugares que
 * regresan stock por FUERA de guardarConImagenes: cancelar un pedido (PedidoServiceImpl,
 * PedidoCancelacionScheduler via deletePedidoById) o cancelar un abono (AbonoServiceImpl) tocan
 * Variantes.stock directo contra el repository, así que antes de esto un cliente con favorito
 * nunca se enteraba de que volvió a haber stock si la reposición venía de una cancelación en vez
 * de una edición manual de la variante.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestockNotificacionService {

    private final IFavoritoRepository iFavoritoRepository;
    private final EmailService emailService;

    /** @param stockAntes stock de la variante justo ANTES del cambio que se acaba de guardar. */
    public void notificarSiRestock(Variantes variante, int stockAntes) {
        if (stockAntes != 0 || variante.getStock() <= 0) return;
        try {
            List<Favorito> favoritos = iFavoritoRepository.findAllByVariante_Id(variante.getId());
            if (favoritos.isEmpty()) return;
            String nombreProducto = variante.getProducto() != null ? variante.getProducto().getNombre() : "un producto";
            String detalleVariante = descripcionVariante(variante);
            for (Favorito favorito : favoritos) {
                Cliente cliente = favorito.getCliente();
                if (cliente == null || !Boolean.TRUE.equals(cliente.getRecibirCorreos())) continue;
                String correo = cliente.getCorreoElectronico();
                if (correo == null || correo.isBlank()) continue;
                emailService.enviarAlertaStock(correo, cliente.getNombrePersona(), nombreProducto, detalleVariante);
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar restock de variante id={}: {}", variante.getId(), e.getMessage());
        }
    }

    private String descripcionVariante(Variantes v) {
        StringBuilder sb = new StringBuilder();
        if (v.getTalla() != null && !v.getTalla().isBlank()) sb.append("Talla ").append(v.getTalla());
        if (v.getColor() != null && !v.getColor().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(v.getColor());
        }
        return sb.toString();
    }
}
