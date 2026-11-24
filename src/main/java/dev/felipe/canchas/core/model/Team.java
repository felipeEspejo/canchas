package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Equipo cliente: quien reserva. Tiene un capitán y un teléfono de contacto. */
public record Team(UUID id, String name, String captain, String phone) {
    private static final Pattern NAME = Pattern.compile("^[\\p{Lu}0-9][\\p{Lu}0-9 .\\-]{2,29}$");
    private static final Pattern PERSON = Pattern.compile("^[\\p{L} .'-]{3,60}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");

    public Team {
        Objects.requireNonNull(id, "El equipo necesita un id");
        name = normalizeName(name);
        if (!NAME.matcher(name).matches()) {
            throw new BusinessRuleException("El nombre del equipo debe tener de 3 a 30 caracteres: letras, números, espacios, punto o guion");
        }
        captain = captain == null ? "" : captain.trim().replaceAll("\\s+", " ");
        if (!PERSON.matcher(captain).matches()) {
            throw new BusinessRuleException("Escribe el nombre del capitán (de 3 a 60 letras)");
        }
        phone = phone == null ? "" : phone.replaceAll("[\\s()-]", "");
        if (!PHONE.matcher(phone).matches()) {
            throw new BusinessRuleException("El teléfono debe tener entre 7 y 15 números");
        }
    }

    /** Mayúsculas y un solo espacio: "los  tigres" y "LOS TIGRES" son el mismo equipo. */
    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public Team withDetails(String newName, String newCaptain, String newPhone) {
        return new Team(id, newName, newCaptain, newPhone);
    }
}
