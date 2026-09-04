package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.Cliente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Detalle completo de un cliente para la pantalla admin de "ver/editar cliente" (clientes/buscar
// -> clientes/mostrar/{id}). Cliente.usuario va con @JsonBackReference (evita el ciclo
// Cliente<->Usuario al serializar), así que buscarPorIdCliente/{id} nunca trae el usuarioId -- y
// sin usuarioId el front no puede guardar (ClienteControllerImpl.save() lo requiere para saber a
// qué usuario pertenece / si ya existe un cliente que actualizar). Este wrapper expone justo lo
// que falta sin tocar el contrato JSON de Cliente que ya consumen otras pantallas.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAdminDetalleDto {
    private Cliente cliente;
    private Integer usuarioId;
    private String username;
}
