package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.policy.PriceQuote;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: la recepción del complejo (cotizar, reservar y cerrar reservas). */
public interface BookingDeskUseCase {

    record BookingRequest(UUID courtId, UUID teamId, LocalDate date, Integer startHour, Integer hours) {
    }

    /** Cualquier filtro en null se ignora. */
    record BookingFilter(LocalDate from, LocalDate to, UUID teamId, BookingStatus status) {
    }

    /** Reserva junto con su cancha y su equipo, lista para mostrar. */
    record BookingView(Booking booking, Court court, Team team) {
    }

    /** Cuánto costaría, hora por hora, sin reservar nada. */
    PriceQuote quote(BookingRequest request);

    Booking book(BookingRequest request);

    Booking markPlayed(UUID bookingId);

    Booking markNoShow(UUID bookingId);

    Booking cancel(UUID bookingId);

    BookingView view(UUID bookingId);

    List<BookingView> search(BookingFilter filter);
}
