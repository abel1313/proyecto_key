package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.AccionDePantalla;

import java.util.List;

/**
 * [Hexagonal: Driven Port] El usuario de prueba con un rol limitado que piden algunas pruebas (R23): "entra con
 * un usuario que solo tenga Tienda → Buscar y el filtro Con stock". Los permisos se dan con los mismos métodos
 * que Gestión de roles.
 */
public interface CuentasPruebaPort {

    /**
     * El rol "ROLE_QA_PRUEBA_{prueba}" con <b>exactamente</b> esas pantallas (por ruta) y acciones, y el usuario
     * "qa.prueba{prueba}" con ese rol, activo, con el correo verificado y los Términos aceptados. La contraseña es
     * aleatoria y no se guarda en ningún lado: el administrador le pone una con "Resetear contraseña" en la
     * pantalla de Usuarios, como con cualquier empleado. Si ya existen, se reusan y sus permisos vuelven a quedar
     * exactos. Regresa el nombre de usuario.
     */
    String usuarioConRol(String prueba, List<String> pantallas, List<AccionDePantalla> acciones);

    /** Desactiva el usuario de prueba de esa prueba (no se borra). Regresa 1 si había uno activo, 0 si no. */
    int quitarUsuarioDeLaPrueba(String prueba);
}
