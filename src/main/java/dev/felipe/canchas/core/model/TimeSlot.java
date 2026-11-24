package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Franja de juego: un día, una hora de inicio en punto y cuántas horas dura.
 * El complejo abre de 8:00 a 23:00 y se reserva por horas completas (de 1 a 3 seguidas).
 */
public record TimeSlot(LocalDate date, int startHour, int hours) {
    public static final int OPENING_HOUR = 8;
    public static final int CLOSING_HOUR = 23;
    public static final int MAX_HOURS = 3;

    public TimeSlot {
        if (date == null) {
            throw new BusinessRuleException("Elige el día de la reserva");
        }
        if (hours < 1 || hours > MAX_HOURS) {
            throw new BusinessRuleException("Una reserva dura de 1 a " + MAX_HOURS + " horas");
        }
        if (startHour < OPENING_HOUR || startHour + hours > CLOSING_HOUR) {
            throw new BusinessRuleException("El complejo atiende de " + OPENING_HOUR + ":00 a " + CLOSING_HOUR
                    + ":00: la reserva debe empezar y terminar dentro de ese horario");
        }
    }

    public int endHour() {
        return startHour + hours;
    }

    public LocalDateTime start() {
        return date.atTime(startHour, 0);
    }

    public LocalDateTime end() {
        return date.atTime(endHour() % 24, 0).plusDays(endHour() / 24);
    }

    public boolean covers(int hour) {
        return hour >= startHour && hour < endHour();
    }

    /** Dos franjas chocan si son el mismo día y comparten al menos una hora. */
    public boolean overlaps(TimeSlot other) {
        return date.equals(other.date) && startHour < other.endHour() && other.startHour < endHour();
    }

    /** Las horas en punto que ocupa: 19:00-21:00 son [19, 20]. */
    public List<Integer> playedHours() {
        return IntStream.range(startHour, endHour()).boxed().toList();
    }

    public String label() {
        return String.format("%02d:00 – %02d:00", startHour, endHour());
    }

    /** Horas del día en las que se puede empezar a jugar: 8, 9, ..., 22. */
    public static List<Integer> openingHours() {
        return IntStream.range(OPENING_HOUR, CLOSING_HOUR).boxed().toList();
    }
}
