package dev.felipe.canchas.adapter.security;

import dev.felipe.canchas.core.port.out.PasswordHashing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Adaptador de salida: cifra contraseñas con BCrypt (el mismo algoritmo que usa el login web). */
public class BCryptHashing implements PasswordHashing {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
