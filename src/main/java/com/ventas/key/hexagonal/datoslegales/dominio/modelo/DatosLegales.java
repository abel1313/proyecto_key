package com.ventas.key.hexagonal.datoslegales.dominio.modelo;

import com.ventas.key.hexagonal.datoslegales.dominio.excepcion.DatosLegalesInvalidosException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Los datos del negocio que la ley pide mostrar antes de comprar.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Reglas (LEGAL_PLAN_DE_ACCION.md, punto 4):
 * <ul>
 *   <li>D1: LFPC art. 76 bis III — antes de comprar, el cliente ve <b>quién vende, domicilio físico,
 *       teléfono</b> y un medio para reclamar (correo). {@link #faltantes()} dice cuáles faltan.</li>
 *   <li>D2: todos son opcionales para guardar (el dueño los va llenando), pero si se escriben tienen
 *       que tener formato válido.</li>
 *   <li>D3: RFC de persona física (13) o moral (12), en mayúsculas.</li>
 *   <li>D4: teléfono de 10 dígitos (se aceptan espacios, guiones y paréntesis al escribirlo).</li>
 *   <li>D5: el texto vacío cuenta como "no capturado".</li>
 * </ul>
 */
public record DatosLegales(
        String nombreResponsable,
        String rfc,
        String domicilio,
        String telefono,
        String correo,
        String horarioAtencion) {

    private static final Pattern RFC = Pattern.compile("^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public DatosLegales {
        nombreResponsable = limpio(nombreResponsable, 150, "El nombre del responsable");
        domicilio = limpio(domicilio, 300, "El domicilio");
        horarioAtencion = limpio(horarioAtencion, 150, "El horario de atención");
        correo = limpio(correo, 150, "El correo");
        rfc = limpio(rfc, 13, "El RFC");
        if (rfc != null) {
            rfc = rfc.toUpperCase();
            if (!RFC.matcher(rfc).matches()) {
                throw new DatosLegalesInvalidosException(
                        "El RFC no es válido: deben ser 13 caracteres (persona física) o 12 (empresa), como viene en tu constancia");
            }
        }
        telefono = limpio(telefono, 30, "El teléfono");
        if (telefono != null) {
            String digitos = telefono.replaceAll("[\\s()+-]", "");
            if (digitos.startsWith("52") && digitos.length() == 12) {
                digitos = digitos.substring(2);
            }
            if (!digitos.matches("\\d{10}")) {
                throw new DatosLegalesInvalidosException("El teléfono tiene que tener 10 dígitos");
            }
            telefono = digitos;
        }
        if (correo != null && !CORREO.matcher(correo).matches()) {
            throw new DatosLegalesInvalidosException("El correo no es válido");
        }
    }

    public static DatosLegales vacios() {
        return new DatosLegales(null, null, null, null, null, null);
    }

    /** D1: lo que falta para cumplir el art. 76 bis III, con el nombre que ve el dueño. */
    public List<String> faltantes() {
        List<String> faltan = new ArrayList<>();
        if (nombreResponsable == null) faltan.add("Nombre del responsable");
        if (domicilio == null) faltan.add("Domicilio");
        if (telefono == null) faltan.add("Teléfono");
        if (correo == null) faltan.add("Correo");
        return faltan;
    }

    public boolean completos() {
        return faltantes().isEmpty();
    }

    private static String limpio(String valor, int maximo, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.trim().replaceAll("\\s+", " ");
        if (v.length() > maximo) {
            throw new DatosLegalesInvalidosException(nombre + " no puede pasar de " + maximo + " caracteres");
        }
        return v;
    }
}
