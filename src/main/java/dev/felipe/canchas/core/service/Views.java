package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingView;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.Team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Arma BookingView (reserva + cancha + equipo) cargando canchas y equipos una sola vez. */
final class Views {
    static final Comparator<Booking> NEWEST_FIRST = Comparator.comparing((Booking b) -> b.slot().start()).reversed();

    private final Map<UUID, Court> courts;
    private final Map<UUID, Team> teams;

    Views(CourtStore courtStore, TeamStore teamStore) {
        this.courts = courtStore.findAll().stream().collect(Collectors.toMap(Court::id, Function.identity()));
        this.teams = teamStore.findAll().stream().collect(Collectors.toMap(Team::id, Function.identity()));
    }

    BookingView of(Booking booking) {
        return new BookingView(booking, courts.get(booking.courtId()), teams.get(booking.teamId()));
    }

    List<BookingView> of(List<Booking> bookings) {
        return bookings.stream().map(this::of).toList();
    }
}
