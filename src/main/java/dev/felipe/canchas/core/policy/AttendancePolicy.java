package dev.felipe.canchas.core.policy;

import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de cumplimiento de los equipos:
 * - Un equipo con 2 o más "no se presentó" en los últimos 30 días queda sancionado y no puede reservar.
 * - Un equipo puede tener como máximo 3 reservas pendientes a futuro (para que nadie acapare la agenda).
 */
public final class AttendancePolicy {
    public static final int NO_SHOWS_FOR_SANCTION = 2;
    public static final int SANCTION_WINDOW_DAYS = 30;
    public static final int MAX_PENDING_BOOKINGS = 3;

    public long recentNoShows(List<Booking> teamBookings, LocalDate today) {
        LocalDate since = today.minusDays(SANCTION_WINDOW_DAYS);
        return teamBookings.stream()
                .filter(b -> b.status() == BookingStatus.NO_SHOW)
                .filter(b -> !b.slot().date().isBefore(since))
                .count();
    }

    public boolean isSanctioned(List<Booking> teamBookings, LocalDate today) {
        return recentNoShows(teamBookings, today) >= NO_SHOWS_FOR_SANCTION;
    }

    public long pendingAhead(List<Booking> teamBookings, LocalDateTime now) {
        return teamBookings.stream()
                .filter(Booking::isPending)
                .filter(b -> b.slot().start().isAfter(now))
                .count();
    }
}
