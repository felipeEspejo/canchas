package dev.felipe.canchas.core;

/** Se pidió algo (cancha, equipo, reserva, cuenta) que no existe. */
public class NotFoundException extends BusinessRuleException {
    public NotFoundException(String message) {
        super(message);
    }
}
