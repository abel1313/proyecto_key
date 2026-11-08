package com.ventas.key.hexagonal.campana.dominio.modelo;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Una persona con código de campaña: el {@code uuid} es suyo para siempre (el mismo en todas las
 * campañas). Se guarda con su consentimiento al aviso de privacidad y se borra cuando el administrador lo pida.
 *
 * @param id {@code null} mientras no se guarda
 */
public record PersonaCampana(Integer id, String uuid, String nombre, String correo, String telefono, boolean consentimiento) {

    public PersonaCampana {
        if (uuid == null || uuid.isBlank()) {
            throw new CampanaInvalidaException("La persona necesita un código.");
        }
        uuid = uuid.trim().toLowerCase();
        nombre = nombre == null ? "" : nombre.trim();
        correo = correo == null || correo.isBlank() ? null : correo.trim().toLowerCase();
        // Se guarda siempre con los últimos 10 dígitos: así «(722) 123-4567» y «+52 722 123 4567» son el mismo teléfono.
        String t = ContactoCliente.ultimos10(telefono);
        telefono = t.isEmpty() ? null : t;
    }

    /** ¿Estos datos de contacto son de esta persona? Mismo correo, o mismo teléfono (últimos 10 dígitos). */
    public boolean coincideCon(ContactoCliente c) {
        if (c == null) {
            return false;
        }
        boolean mismoCorreo = correo != null && c.correo() != null && correo.equals(c.correo());
        boolean mismoTelefono = telefono != null && telefono.equals(c.telefono());
        return mismoCorreo || mismoTelefono;
    }
}
