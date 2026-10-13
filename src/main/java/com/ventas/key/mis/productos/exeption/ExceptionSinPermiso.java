package com.ventas.key.mis.productos.exeption;

// La sesión es válida pero ese recurso no es suyo (ej. reclamar una compra hecha con otro correo).
// Sale como 403. Para "no tiene la pantalla o la acción" ya está Spring Security (también 403).
public class ExceptionSinPermiso extends RuntimeException {
    public ExceptionSinPermiso(String message) {
        super(message);
    }
}
