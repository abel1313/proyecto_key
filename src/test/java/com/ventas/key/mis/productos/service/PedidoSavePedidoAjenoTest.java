package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.exeption.ExceptionSinPermiso;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Auditoria de seguridad 2026-11-03: un usuario comun ya no puede generar pedidos a nombre de otro cliente por savePedido. */
class PedidoSavePedidoAjenoTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("un cliente comun no genera un pedido a nombre de otro cliente (403)")
    void noAPedidoAjeno() {
        Cliente delOtro = new Cliente();
        delOtro.setId(5);
        delOtro.setCorreoVerificado(true);
        delOtro.setDatosCompletos(true);
        IClienteRepository repo = Mockito.mock(IClienteRepository.class);
        when(repo.findById(5)).thenReturn(Optional.of(delOtro));

        Cliente miCliente = new Cliente();
        miCliente.setId(9);
        Usuario yo = new Usuario();
        yo.setId(2);
        yo.setCliente(miCliente);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                yo, "t", List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));

        PedidoServiceImpl servicio = Mockito.mock(PedidoServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(servicio, "iClienteRepository", repo);

        var pedido = new com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido();
        var dto = new com.ventas.key.mis.productos.models.pedidos.ClienteDTOPedido();
        dto.setId(5);
        pedido.setCliente(dto);
        pedido.setEstadoPedido("Entregado");

        assertThatThrownBy(() -> servicio.savePedido(pedido, null))
                .isInstanceOf(ExceptionSinPermiso.class)
                .hasMessageContaining("otro cliente");
    }
}
