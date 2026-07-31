package com.ventas.key.mis.productos.exeption;

/**
 * Codigo de 6 digitos (reset de password / verificacion de correo) invalido, expirado o con los
 * intentos agotados.
 *
 * Existe como tipo propio para poder marcarla en {@code @Transactional(noRollbackFor = ...)}: el
 * contador de intentos fallidos se guarda en la misma transaccion que lanza la excepcion, y sin
 * eso el rollback borraria el incremento y el contador nunca avanzaria.
 */
public class ExceptionCodigoInvalido extends RuntimeException {
    public ExceptionCodigoInvalido(String message) {
        super(message);
    }
}
