package dev.felipe.canchas.core;

/**
 * Una regla del negocio no se cumple (por ejemplo, la cancha ya está reservada a esa hora).
 * El mensaje está escrito para mostrárselo tal cual a la persona que usa el sistema.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
