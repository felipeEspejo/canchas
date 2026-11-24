package dev.felipe.canchas.core.model;

/**
 * Ciclo de vida de una reserva:
 * RESERVED -> PLAYED (se jugó y se cobró) | NO_SHOW (el equipo no llegó) | CANCELLED (se canceló a tiempo).
 */
public enum BookingStatus {
    RESERVED("Reservada"),
    PLAYED("Jugada"),
    NO_SHOW("No se presentó"),
    CANCELLED("Cancelada");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Mientras la reserva esté vigente o ya se haya jugado, nadie más puede usar esa cancha a esa hora. */
    public boolean holdsCourt() {
        return this == RESERVED || this == PLAYED;
    }
}
