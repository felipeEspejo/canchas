package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cancha del complejo. No guarda si está "ocupada": eso depende de la hora y lo deciden las reservas.
 * Una cancha que ya no se usa no se borra (perderíamos el historial): se desactiva.
 */
public record Court(UUID id, String code, String name, CourtFormat format, boolean active) {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9-]{1,9}$");

    public Court {
        Objects.requireNonNull(id, "La cancha necesita un id");
        code = code == null ? "" : code.trim().toUpperCase();
        if (!CODE.matcher(code).matches()) {
            throw new BusinessRuleException("El código de la cancha debe tener de 2 a 10 caracteres: letras, números o guion (ejemplo F5-A)");
        }
        name = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 40) {
            throw new BusinessRuleException("El nombre de la cancha debe tener entre 3 y 40 caracteres");
        }
        if (format == null) {
            throw new BusinessRuleException("Elige el formato de la cancha (fútbol 5 o fútbol 7)");
        }
    }

    public static Court open(UUID id, String code, String name, CourtFormat format) {
        return new Court(id, code, name, format, true);
    }

    public Court withDetails(String newCode, String newName, CourtFormat newFormat) {
        return new Court(id, newCode, newName, newFormat, active);
    }

    public Court activate() {
        if (active) {
            throw new BusinessRuleException("La cancha " + code + " ya está activa");
        }
        return new Court(id, code, name, format, true);
    }

    public Court deactivate() {
        if (!active) {
            throw new BusinessRuleException("La cancha " + code + " ya está fuera de servicio");
        }
        return new Court(id, code, name, format, false);
    }
}
