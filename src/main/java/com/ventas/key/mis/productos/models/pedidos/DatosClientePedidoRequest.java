package com.ventas.key.mis.productos.models.pedidos;

import lombok.Data;

/** Los datos del cliente que se agregan a un pedido que no tenia cliente (solo el administrador). Correo y telefono son opcionales. */
@Data
public class DatosClientePedidoRequest {
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    /** Opcionales: los mismos del formulario de Venta directa. */
    private String segundoNombre;
    private String apellidoMaterno;
    /** yyyy-MM-dd, o vacio. */
    private String fechaNacimiento;
    private String sexo;
}
