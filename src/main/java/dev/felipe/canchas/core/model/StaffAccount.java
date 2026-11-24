package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Cuenta del personal que usa el sistema (recepción o administración). Guarda la contraseña ya cifrada. */
public record StaffAccount(UUID id, String username, String passwordHash, StaffRole role) {
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_.-]{3,30}$");

    public StaffAccount {
        Objects.requireNonNull(id, "La cuenta necesita un id");
        username = username == null ? "" : username.trim();
        if (!USERNAME.matcher(username).matches()) {
            throw new BusinessRuleException("El usuario debe tener de 3 a 30 caracteres (letras, números, . _ -)");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new BusinessRuleException("La cuenta necesita una contraseña");
        }
        if (role == null) {
            throw new BusinessRuleException("Elige el rol de la cuenta");
        }
    }

    public StaffAccount withRole(StaffRole newRole) {
        return new StaffAccount(id, username, passwordHash, newRole);
    }

    public StaffAccount withPasswordHash(String newHash) {
        return new StaffAccount(id, username, newHash, role);
    }
}
