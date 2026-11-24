package dev.felipe.canchas.core.policy;

/** Franja de precio de una hora de juego. */
public enum RateBand {
    DAY("Diurna"),
    NIGHT("Nocturna"),
    WEEKEND("Fin de semana");

    private final String label;

    RateBand(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
