package dev.felipe.canchas.core.port.out;

/** Puerto de salida: cifrar y comprobar contraseñas (el core no sabe qué algoritmo se usa). */
public interface PasswordHashing {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
