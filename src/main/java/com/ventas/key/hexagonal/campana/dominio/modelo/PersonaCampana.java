package com.ventas.key.hexagonal.campana.dominio.modelo;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Una persona con código de campaña: el {@code uuid} es suyo para siempre (el mismo en todas las
 * campañas). Se guarda con su consentimiento al aviso de privacidad y se borra cuando el administrador lo pida.
 *
 * @param id {@code null} mientras no se guarda
 * @param clienteId cliente registrado al que pertenece el código (o {@code null})
 * @param clienteSinRegistroId cliente sin registro con el que se hizo la primera compra (o {@code null})
 */
public record PersonaCampana(Integer id, String uuid, String nombre, String correo, String telefono, boolean consentimiento,
                             Integer clienteId, Integer clienteSinRegistroId) {

    /** Sin cliente ligado (personas anteriores al ligado con el cliente). */
    public PersonaCampana(Integer id, String uuid, String nombre, String correo, String telefono, boolean consentimiento) {
        this(id, uuid, nombre, correo, telefono, consentimiento, null, null);
    }

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

    /** Una copia con el cliente ligado (solo rellena lo que falta). */
    public PersonaCampana conCliente(Integer nuevoClienteId, Integer nuevoSinRegistroId) {
        return new PersonaCampana(id, uuid, nombre, correo, telefono, consentimiento,
                clienteId != null ? clienteId : nuevoClienteId, clienteSinRegistroId != null ? clienteSinRegistroId : nuevoSinRegistroId);
    }

    /** ¿Estos datos son de esta persona? El mismo cliente, el mismo correo o el mismo teléfono (últimos 10 dígitos). */
    public boolean coincideCon(ContactoCliente c) {
        if (c == null) {
            return false;
        }
        boolean mismoCorreo = correo != null && c.correo() != null && correo.equals(c.correo());
        boolean mismoTelefono = telefono != null && telefono.equals(c.telefono());
        boolean mismoCliente = c.clienteId() != null && c.clienteId().equals(clienteId);
        boolean mismoSinRegistro = c.clienteSinRegistroId() != null && c.clienteSinRegistroId().equals(clienteSinRegistroId);
        return mismoCorreo || mismoTelefono || mismoCliente || mismoSinRegistro;
    }
}
