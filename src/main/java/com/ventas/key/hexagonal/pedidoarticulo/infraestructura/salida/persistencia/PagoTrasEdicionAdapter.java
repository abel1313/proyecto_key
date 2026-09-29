package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PagoTrasEdicionPort;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Usa {@code AbonoServiceImpl}, que es quien crea y borra la venta de un pedido a credito: dos
 * caminos que liquidan serian dos ventas distintas.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 */
@Component
@RequiredArgsConstructor
public class PagoTrasEdicionAdapter implements PagoTrasEdicionPort {

    private final IAbonoService abonoService;

    @Override
    public void ajustarTrasEditar(Integer pedidoId) {
        Integer usuarioId = AuthenticationUtils.currentUsuarioOpt().map(Usuario::getId).orElse(null);
        abonoService.ajustarTrasEditarArticulos(pedidoId, usuarioId);
    }
}
