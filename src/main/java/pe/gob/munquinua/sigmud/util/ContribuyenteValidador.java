package pe.gob.munquinua.sigmud.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.model.Contribuyente;

/** Valida un Contribuyente y devuelve la lista de errores en español (vacía si todo está bien). */
public final class ContribuyenteValidador {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final int MIN_NOMBRE = 3;
    private static final int MAX_NOMBRE = 150;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_DIRECCION = 200;

    private ContribuyenteValidador() { }

    public static List<String> validar(Contribuyente c) {
        List<String> errores = new ArrayList<>();
        if (c == null) {
            errores.add("No se recibieron los datos del contribuyente.");
            return errores;
        }
        validarDocumento(c, errores);
        validarNombre(c, errores);
        validarContacto(c, errores);
        validarFechaNacimiento(c, errores);
        return errores;
    }

    /** DNI (8 dígitos) para NATURAL; RUC (11 dígitos, empieza con 10 o 20) para JURIDICA. */
    private static void validarDocumento(Contribuyente c, List<String> errores) {
        if (Contribuyente.NATURAL.equals(c.getTipoPersona())) {
            if (!Validador.dniValido(c.getDniRuc())) {
                errores.add("El DNI debe tener exactamente 8 dígitos.");
            }
        } else if (Contribuyente.JURIDICA.equals(c.getTipoPersona())) {
            if (!Validador.rucValido(c.getDniRuc())) {
                errores.add("El RUC debe tener 11 dígitos y empezar con 10 o 20.");
            }
        } else {
            errores.add("Seleccione el tipo de persona (natural o jurídica).");
        }
    }

    private static void validarNombre(Contribuyente c, List<String> errores) {
        String nombre = c.getNombresRazonSocial() == null ? "" : c.getNombresRazonSocial().trim();
        if (nombre.length() < MIN_NOMBRE) {
            errores.add("El nombre o razón social es obligatorio (mínimo 3 caracteres).");
        } else if (nombre.length() > MAX_NOMBRE) {
            errores.add("El nombre o razón social no puede pasar de 150 caracteres.");
        }
    }

    /** Teléfono, correo y dirección son opcionales; si se escriben, deben ser válidos. */
    private static void validarContacto(Contribuyente c, List<String> errores) {
        if (!Validador.vacio(c.getTelefono()) && !Validador.telefonoValido(c.getTelefono())) {
            errores.add("El teléfono debe tener 9 dígitos y empezar con 9.");
        }
        if (!Validador.vacio(c.getEmail())
                && (!Validador.emailValido(c.getEmail()) || c.getEmail().trim().length() > MAX_EMAIL)) {
            errores.add("El correo electrónico no es válido.");
        }
        if (c.getDireccion() != null && c.getDireccion().trim().length() > MAX_DIRECCION) {
            errores.add("La dirección no puede pasar de 200 caracteres.");
        }
    }

    /** Solo para personas naturales; no puede ser futura ni anterior a 1900. */
    private static void validarFechaNacimiento(Contribuyente c, List<String> errores) {
        LocalDate f = c.getFechaNacimiento();
        if (f == null) {
            return;
        }
        if (Contribuyente.JURIDICA.equals(c.getTipoPersona())) {
            errores.add("La fecha de nacimiento solo corresponde a personas naturales.");
        } else if (f.isAfter(LocalDate.now())) {
            errores.add("La fecha de nacimiento no puede ser futura.");
        } else if (f.isBefore(FECHA_MINIMA)) {
            errores.add("La fecha de nacimiento no puede ser anterior a 1900.");
        }
    }
}
