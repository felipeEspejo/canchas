package dev.felipe.canchas.core.model;

/** Formato de juego de una cancha. Cada formato tiene su propio plan de tarifas. */
public enum CourtFormat {
    FUTBOL_5("Fútbol 5", 10),
    FUTBOL_7("Fútbol 7", 14);

    private final String label;
    private final int players;

    CourtFormat(String label, int players) {
        this.label = label;
        this.players = players;
    }

    public String getLabel() {
        return label;
    }

    /** Jugadores en cancha sumando los dos equipos. */
    public int getPlayers() {
        return players;
    }
}
